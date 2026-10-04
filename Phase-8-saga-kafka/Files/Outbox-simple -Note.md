# Outbox Pattern: Simple Note

## 1. The problem

Saving to the database and sending to Kafka are two separate actions. If the service crashes between them, the data is saved but the event is lost.

## 2. How the outbox fixes it

```
Booking Service
 ┌──────────────────────────────────────────────┐
 │  ONE database transaction                    │
 │   1. update bookings table (status changes)  │
 │   2. insert row into outbox_events (JSON)    │
 └──────────────────────────────────────────────┘
          │ committed together, or not at all
          ▼
 OutboxPublisher (runs every 1 second)
   - reads rows with status PENDING
   - sends each row to Kafka
   - marks the row SENT after Kafka confirms
          ▼
        Kafka topic  -->  other services
```

- **Never lost:** the event is in the database as soon as the booking changes. If Booking crashes or Kafka is down, the row stays `PENDING` and is sent later.
- **Maybe duplicated:** if the publisher sends and crashes before marking `SENT`, it sends again. So delivery is at-least-once, and consumers must be idempotent (Step 8).

## 3. Example: payment fails

Booking receives `payment-failed` and runs `BookingService.markPaymentFailed`.

**A. Java object to JSON text** (`OutboxService.save`)

```java
new BookingFailedEvent(eventId, 4L, 1L, List.of(10L, 11L),
        "rahul@test.com", "Amount exceeds the allowed limit of 1000")
```

becomes

```json
{"eventId":"a1b2","bookingId":4,"showId":1,"seatIds":[10,11],
 "customerEmail":"rahul@test.com","reason":"Amount exceeds the allowed limit of 1000"}
```

**B. Outbox row in Postgres**

- `topic`: `booking-failed`
- `message_key`: `4` (booking id, keeps one booking's events in order)
- `payload`: the JSON text
- `status`: `PENDING`

The class name is not stored. Only text.

**C. Sending** (`OutboxPublisher`)

```java
JsonNode payload = jsonMapper.readTree(row.getPayload());   // text -> generic JSON tree
kafkaTemplate.send(row.getTopic(), row.getMessageKey(), payload);
```

`JsonNode` is a tree of fields with no class. Because `add.type.headers` is `false`, no class name is attached to the message.

**D. On Kafka**

Just JSON bytes on the topic `booking-failed`.

**E. JSON back to a Java class** (consumer)

```java
@KafkaListener(topics = "booking-failed",
        properties = "spring.json.value.default.type=com.rahul.notificationservice.kafka.event.BookingFailedEvent")
public void onBookingFailed(@Payload BookingFailedEvent event) { ... }
```

Jackson fills the consumer's own class by matching field names:

- `bookingId` -> `getBookingId()`
- `customerEmail` -> `getCustomerEmail()`
- `reason` -> `getReason()`

Notification's class has no `showId` or `seatIds`, so those are ignored. Cinema's class has `seatIds`, so it uses them from the same message.

## 4. Rules to remember

1. One topic carries one event type. The topic tells the consumer which class to use.
2. JSON field names are the contract. A mismatched name arrives as `null`, with no error.
3. Each service owns its own event class and keeps only the fields it needs.
4. The outbox is generic. `OutboxPublisher` has one loop for all events and needs only topic, key and JSON.
5. `OutboxService.save` uses `Propagation.MANDATORY`. Called outside a transaction, it throws, which protects the guarantee.

## 5. Classes involved (Booking)

- `outbox.OutboxEvent`, `OutboxRepository`, `OutboxService`: store and read outbox rows
- `kafka.publisher.OutboxPublisher`: scheduled job that sends rows to Kafka
- `service.BookingService`: changes status and saves the event in one transaction
- `db/migration/V6__create_outbox_events_table.sql`: the table

## 6. Interview line

"The outbox saves the event as JSON in the same transaction as the data change. A scheduled publisher sends pending rows to Kafka and marks them sent. That gives at-least-once delivery with no lost events, so consumers are made idempotent."
