# Notification System Design

## Stage 1

### Core Actions
The notification platform supports three core actions:
- Sending notifications to students
- Fetching notifications for a student
- Marking notifications as read

### REST API Endpoints

#### Get All Notifications
- Method: GET
- Route: /notifications
- Headers: Authorization: Bearer <token>
- Response (200):
{
  "notifications": [
    {
      "ID": "uuid",
      "Type": "Placement",
      "Message": "Company hiring",
      "Timestamp": "2026-04-22T17:51:30",
      "isRead": false
    }
  ]
}

#### Get Notification by ID
- Method: GET
- Route: /notifications/:id
- Headers: Authorization: Bearer <token>
- Response (200):
{
  "ID": "uuid",
  "Type": "Placement",
  "Message": "Company hiring",
  "Timestamp": "2026-04-22T17:51:30",
  "isRead": false
}

#### Mark Notification as Read
- Method: PUT
- Route: /notifications/:id/read
- Headers: Authorization: Bearer <token>
- Response (200):
{
  "message": "Notification marked as read"
}

#### Send Notification
- Method: POST
- Route: /notifications
- Headers: Authorization: Bearer <token>
- Request:
{
  "Type": "Placement",
  "Message": "Company XYZ is hiring"
}
- Response (201):
{
  "message": "Notification sent successfully"
}

### Real-time Notifications
I would use WebSockets for real-time delivery. When a new notification is created, the server pushes it instantly to all connected students without requiring a page refresh.

---

## Stage 2

### Database Choice
I would use PostgreSQL because the data is structured, relationships between students and notifications are clear, and it supports complex queries like filtering by type and sorting by timestamp efficiently.

### DB Schema

CREATE TABLE students (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE notifications (
  id UUID PRIMARY KEY,
  student_id UUID REFERENCES students(id),
  notification_type VARCHAR(50) CHECK (notification_type IN ('Event', 'Result', 'Placement')),
  message TEXT NOT NULL,
  is_read BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT NOW()
);

### Problems at Scale
As data grows to millions of notifications, the main problems would be:
- Full table scans becoming slow without indexes
- Too many concurrent reads overwhelming the database
- Storage growing very large over time

### Solutions
- Add indexes on frequently queried columns
- Use Redis caching for repeated reads
- Archive old notifications to separate table

---

## Stage 3

### Why the Query is Slow
SELECT * FROM notifications
WHERE studentID = 1042 AND isRead = false
ORDER BY createdAt DESC;

This query is slow because there are no indexes on studentID, isRead, or createdAt. With 5 million rows, PostgreSQL does a full table scan every time which is very expensive.

### Fix
CREATE INDEX idx_notifications_student_unread
ON notifications(student_id, is_read, created_at DESC);

This composite index covers all three conditions in the query and makes it run in milliseconds instead of seconds.

### Should We Index Every Column?
No. Indexing every column is bad advice. Indexes speed up reads but slow down writes because every INSERT or UPDATE must also update all indexes. With high write volume, this would make the system very slow.

### Query to Find Students with Placement Notifications in Last 7 Days
SELECT DISTINCT student_id
FROM notifications
WHERE notification_type = 'Placement'
AND created_at >= NOW() - INTERVAL '7 days';

---

## Stage 4

### Problem
Fetching notifications on every page load puts constant pressure on the database. With 50,000 students loading pages frequently, the DB gets overwhelmed.

### Solution — Redis Caching
- On first request: fetch from DB, store result in Redis with TTL of 60 seconds
- On subsequent requests: return directly from Redis, DB not touched
- On new notification: invalidate the cache so next request fetches fresh data

### Tradeoffs
- Adds Redis as extra infrastructure to manage
- Students may see slightly stale data for up to 60 seconds
- Greatly reduces DB load and improves response time

---

## Stage 5

### Problems with Current Implementation
function notify_all(student_ids, message):
  for student_id in student_ids:
    send_email(student_id, message)
    save_to_db(student_id, message)
    push_to_app(student_id, message)

Problems:
- Sequential processing of 50,000 students is very slow
- If send_email fails at student 200, remaining 49,800 students get no email
- No retry mechanism for failures
- Email and DB save are tightly coupled — if one fails, state is inconsistent

### Redesigned with Message Queue
function notify_all(student_ids, message):
  for student_id in student_ids:
    push_to_queue({student_id, message})

queue_worker (runs in parallel):
  job = pick_from_queue()
  save_to_db(job.student_id, job.message)
  send_email(job.student_id, job.message)
  push_to_app(job.student_id, job.message)
  if any step fails: retry job

### Why This is Better
- Multiple workers process jobs in parallel — much faster
- Failed jobs are retried automatically — no student is missed
- DB save and email are handled together in one atomic job
- System stays consistent even if email service is temporarily down

---

## Stage 6

### Approach — Priority Inbox
To find top N notifications by priority, I combine two signals:

1. Weight based on type:
   - Placement = 3 (highest priority)
   - Result = 2
   - Event = 1 (lowest priority)

2. Recency score based on timestamp — newer notifications get higher score

Final priority score = weight + recency score

Notifications are sorted by this score in descending order and top N are returned.

To handle new notifications efficiently as they keep coming in, I use a max-heap (priority queue) so insertion and retrieval of top N is O(log n) instead of sorting the entire list every time.