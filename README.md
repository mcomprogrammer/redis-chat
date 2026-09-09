# Redis Chat

A Spring Boot chat application using Redis for rooms, participants, message history, and realtime updates through Redis Pub/Sub and SSE.

## Work split

I wrote RedisChatService, the DTOs, exceptions, GlobalExceptionHandler, and ChatController by hand.

RedisPubSubConfig, ChatSseService, and the tests were made with AI support based on my high-level design and step-by-step handholding.

## Main flow

1. Create a room.
2. Join a participant.
3. Send messages.
4. Read chat history or subscribe to realtime messages.

Only participants who joined a room can send messages or subscribe to its stream.

## API

| Method | Endpoint | Purpose |
|---|---|---|
| POST | /api/chatapp/chatrooms | Create a room |
| POST | /api/chatapp/chatrooms/{roomId}/join | Join a room |
| POST | /api/chatapp/chatrooms/{roomId}/messages | Send a message |
| GET | /api/chatapp/chatrooms/{roomId}/messages?limit=10 | Get history |
| GET | /api/chatapp/chatrooms/{roomId}/stream?participant=alice | Receive realtime messages |

## Redis design

- Room: Hash at chat:room:{roomId}
- Participants: Set at chat:room:{roomId}:participants
- Messages: List at chat:room:{roomId}:messages
- Realtime events: Pub/Sub channel chat:room:{roomId}:events

## Code structure

- RedisChatService: room, membership, message, and history operations
- ChatController: endpoints and validation
- DTOs: API request and response models
- Exceptions and GlobalExceptionHandler: application error responses
- ChatSseService and RedisPubSubConfig: SSE and Redis Pub/Sub wiring

## Run

Requires JDK 21 or newer and Redis at localhost:6379. With Docker: `docker run --name redis-chat -p 6379:6379 -d redis:7`. If already created, use `docker start redis-chat`.

~~~powershell
.\mvnw.cmd spring-boot:run
~~~

App URL: http://localhost:8080. On Linux/macOS use `sh mvnw spring-boot:run`.

Import [the Postman collection](postman/RedisChat.postman_collection.json), then run requests 1–4 in order. For SSE, leave request 5 open and send request 3 again from another tab. Change `roomId` for a fresh room; creating it twice returns 409.

Run tests (Redis must be running):

~~~powershell
.\mvnw.cmd test
~~~

