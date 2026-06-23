# Notification System Design

## Stage 1

### Core Actions
The notification platform needs to support three main actions:
- Sending notifications to students
- Fetching notifications for a logged-in student
- Marking a notification as read

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
For real-time delivery I would go with WebSockets. The idea is simple — once a student logs in, a WebSocket connection stays open. Whenever a new notification is created on the server side, it gets pushed directly to the student without them needing to refresh the page. This is much better than polling the server every few seconds.

---

## Stage 2

### Database Choice
I would go with PostgreSQL here. The data is clearly relational — students have notifications, notifications have types and timestamps. NoSQL like MongoDB didn't seem necessary since we don't have unstructured or highly variable data. PostgreSQL also handles complex queries like filtering by type and ordering by timestamp very well.

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
As the data grows to millions of notifications, a few problems will come up:
- Queries without indexes will do full table scans and become very slow
- Too many students hitting the DB at the same time will overwhelm it
- Storage will keep growing and old data will slow things down

### Solutions
- Add indexes on columns we filter and sort by frequently
- Use Redis to cache repeated reads so the DB isn't hit every time
- Archive old notifications to a separate table periodically

---

## Stage 3

### Why the Query is Slow
SELECT * FROM notifications
WHERE studentID = 1042 AND isRead = false
ORDER BY createdAt DESC;

This is slow because there are no indexes on studentID, isRead or createdAt. With 5 million rows PostgreSQL ends up scanning the entire table every single time which obviously won't scale at all.

### Fix
CREATE INDEX idx_notifications_student_unread
ON notifications(student_id, is_read, created_at DESC);

This composite index covers all three conditions at once. After adding this the query should run in milliseconds instead of several seconds.

### Should We Index Every Column?
No, that's not good advice at all. Indexes help reads but they slow down writes because every INSERT or UPDATE has to update all the indexes too. If we index every column and have high write volume the system will actually get slower overall. We should only index columns we actually query on.

### Query to Find Students with Placement Notifications in Last 7 Days
SELECT DISTINCT student_id
FROM notifications
WHERE notification_type = 'Placement'
AND created_at >= NOW() - INTERVAL '7 days';

---

## Stage 4

### Problem
Every time a student loads the page, the app hits the database to fetch their notifications. With 50,000 students doing this frequently the DB just can't keep up and response times get bad.

### Solution — Redis Caching
I initially thought about pagination but caching felt more impactful here since the same notification data is being fetched again and again by the same student.

The approach:
- First request: fetch from DB, store in Redis with a TTL of 60 seconds
- Subsequent requests within 60 seconds: serve directly from Redis, DB not touched
- When a new notification arrives: invalidate that student's cache so they get fresh data on next load

### Tradeoffs
- Redis adds extra infrastructure to set up and maintain
- There's a small window where students might see slightly stale data (up to 60 seconds)
- But the DB load drops dramatically and response times improve a lot — worth it

---

## Stage 5

### Problems with Current Implementation
function notify_all(student_ids, message):
for student_id in student_ids:
send_email(student_id, message)
save_to_db(student_id, message)
push_to_app(student_id, message)

The problems I see:
- It processes 50,000 students one by one sequentially — this will take forever
- If send_email fails at student 200, the remaining 49,800 students never get notified
- There's no retry mechanism anywhere
- Email sending and DB save are tightly coupled — if the email API is down, nothing gets saved either

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

### Why This Works Better
- Multiple workers pull from the queue in parallel so it's much faster
- If email fails for a student, that job gets retried automatically — no one is missed
- DB save and email are handled together per job so state stays consistent
- Even if the email service goes down temporarily, jobs just wait in the queue and get processed once it's back up

---

## Stage 6

### Approach — Priority Inbox
To find the top N notifications by priority I combine two signals:

1. Weight based on notification type:
  - Placement = 3 (most urgent for students)
  - Result = 2
  - Event = 1

2. Recency — newer notifications should rank higher than older ones of the same type

Final priority score = type weight + recency score, sorted descending, top N returned.

To handle new notifications coming in continuously without re-sorting everything, I use a max-heap (priority queue). Insertion and retrieval of top N stays O(log n) which is efficient even as the notification count grows.