# ☕ httpServer

> A lightweight, multithreaded HTTP server built from scratch in Java — no frameworks, no magic, just raw sockets and deliberate design.

---

## What It Does

`httpServer` is a hand-rolled HTTP/1.1 server that handles real web traffic using Java's `ServerSocket` API. It parses raw HTTP requests, routes them to static files or handlers, and responds with properly formatted HTTP responses — all without a single dependency on Spring, Jetty, or Tomcat.

It supports:
- **Static file serving** (HTML, CSS, JS) with correct `Content-Type` headers
- **Dynamic routing** with named path parameters (e.g. `/about/:id`)
- **Query string parsing** (e.g. `/search?q=hello&page=2`)
- **POST body parsing** from `application/x-www-form-urlencoded` forms
- **Authorization middleware** via the `Authorization` header
- **CORS preflight handling** (`OPTIONS` method)
- **Concurrent connections** via a fixed thread pool (10 threads)

---

## Architecture

```
                         ┌─────────────────────┐
                         │   ServerSocket :8080  │
                         └────────┬────────────┘
                                  │ accept()
                         ┌────────▼────────────┐
                         │  ExecutorService     │
                         │  (10-thread pool)    │
                         └────────┬────────────┘
                                  │ submit()
                         ┌────────▼────────────┐
                         │   handleClient()     │
                         │  ┌────────────────┐  │
                         │  │ Parse Request  │  │
                         │  │ Extract Headers│  │
                         │  │ Auth Middleware│  │
                         │  │ Route Resolve  │  │
                         │  └───────┬────────┘  │
                         └──────────┼───────────┘
               ┌───────────┬────────┴──────┬──────────────┐
               ▼           ▼               ▼              ▼
           GET           POST            PUT           DELETE
        showUI()      handlePost()  handleByIdPut()  handleByIdDelete()
```

---

## Routing

Routes are registered in `addRoutes()` and resolved by the `Handler` class:

```java
router.addRoute("/",          "static/index.html");
router.addRoute("/about",     "static/about.html");
router.addRoute("/form",      "static/form.html");
router.addRoute("/about/:id", "static/index.html");  // :id becomes a path param
```

Named segments like `:id` are automatically extracted and available on `req.params`.

---

## Request Lifecycle

Every incoming connection goes through this pipeline:

1. **Read** the first line → extract `method` and `route`
2. **Parse headers** → populate `req.headers`
3. **Preflight check** → `OPTIONS` requests are short-circuited with CORS headers
4. **Authorization** → reject with `401` if `Authorization` header is missing
5. **Route resolution** → match path, extract params and query strings
6. **Dispatch** → hand off to the appropriate method handler

---

## Endpoints

| Method   | Path         | Description                              |
|----------|--------------|------------------------------------------|
| `GET`    | `/`          | Serves `static/index.html`               |
| `GET`    | `/about`     | Serves `static/about.html`               |
| `GET`    | `/form`      | Serves `static/form.html`                |
| `GET`    | `/about/:id` | Serves index with `:id` as a path param  |
| `POST`   | any route    | Parses form body, echoes back as JSON    |
| `PUT`    | `/:id`       | Stub for update logic (id via params)    |
| `DELETE` | `/:id`       | Removes entry by id from in-memory list  |
| `OPTIONS`| any route    | CORS preflight — returns `204 No Content`|

---

## Authorization

Every non-`OPTIONS` request is checked for an `Authorization` header. Missing it returns:

```json
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{"error":"Unauthorized"}
```

---

## Static File Serving

Files are read from disk with `Files.readAllBytes()` and served with the correct `Content-Type`:

| Extension | Content-Type               |
|-----------|----------------------------|
| `.css`    | `text/css`                 |
| `.js`     | `application/javascript`   |
| anything else | `text/html`           |

---

## Running the Server

**Prerequisites:** Java 11+

```bash
# Compile
javac httpServer.java

# Run
java httpServer
```

The server starts on **port 8080**. Open `http://localhost:8080` in your browser.

Make sure your `static/` directory exists with at least `index.html`, `about.html`, and `form.html`.

---

## Project Structure

```
.
├── httpServer.java          # Core server logic
├── RouteHandler/
│   └── Handler.java         # Route registration & resolution
├── Wrapper/
│   └── Wrapper.java         # Exception-wrapping utility (Runnable → checked)
└── static/
    ├── index.html
    ├── about.html
    └── form.html
```

---

## Known Limitations & Future Work

- **In-memory storage only** — `data` list resets on every restart; no persistence layer
- **PUT handler is stubbed** — update logic is intentionally left as a placeholder
- **No HTTPS** — runs plain HTTP; add TLS via `SSLServerSocket` for production use
- **No request body streaming** — reads entire body into memory; not suitable for large uploads
- **Basic auth check** — only validates presence of `Authorization` header, not the value

---

## Design Goals

This project is a study in **building from primitives**. The goal was never to replace a production server — it was to understand what production servers actually do: parse bytes, match patterns, manage concurrency, and speak HTTP. Every abstraction here was written by hand on purpose.

---

*Built with Java standard library only. No frameworks were harmed in the making of this server.*
