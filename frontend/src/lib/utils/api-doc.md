# SWE574 Group2 API Documentation

**Version:** `v0.1`  
**Servers:**  
- **Base URL:** `http://localhost:8080` (SWE574 Group2 APIs)

---

## Table of Contents

1. [Endpoints](#endpoints)
   1. [Post Controller](#post-controller)
   2. [Notification Controller](#notification-controller)
   3. [Mystery Object Image Controller](#mystery-object-image-controller)
   4. [Comment Controller](#comment-controller)
   5. [User Controller](#user-controller)
2. [Schemas (Data Models)](#schemas-data-models)
   - [PostCreationDto](#postcreationdto)
   - [CommentCreateDto](#commentcreatedto)
   - [PostDetailsDto](#postdetailsdto)
   - [CommentDetailsDto](#commentdetailsdto)
   - [MysteryObject](#mysteryobject)
   - [NotificationDto](#notificationdto)
   - [User](#user)
   - [PostListDto](#postlistdto)
   - [Pageable](#pageable)
   - [PagedModel](#pagedmodel)
   - [PagedModelPostListDto](#pagedmodelpostlistdto)
   - [PageMetadata](#pagemetadata)

---

## Endpoints

### Post Controller

The **Post Controller** manages operations related to posts, including creating, listing, searching, voting, and marking best answers.

1. **Mark Best Answer**
   - **Endpoint:** `PUT /api/posts/{postId}/markBestAnswer/{commentId}`
   - **Operation ID:** `markBestAnswer`
   - **Parameters:**
     | Name      | In   | Type    | Required | Description                       |
     |-----------|------|---------|----------|-----------------------------------|
     | postId    | path | integer | yes      | ID of the post                    |
     | commentId | path | integer | yes      | ID of the comment to mark as best |
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: `type: object` (key-value pairs of integers)

2. **Upvote Post**
   - **Endpoint:** `POST /api/posts/upvote/{postId}`
   - **Operation ID:** `upvotePost`
   - **Parameters:**
     | Name   | In   | Type    | Required | Description          |
     |--------|------|---------|----------|----------------------|
     | postId | path | integer | yes      | ID of the post       |
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: `type: object` (key-value pairs of integers)

3. **Downvote Post**
   - **Endpoint:** `POST /api/posts/downvote/{postId}`
   - **Operation ID:** `downvotePost`
   - **Parameters:**
     | Name   | In   | Type    | Required | Description          |
     |--------|------|---------|----------|----------------------|
     | postId | path | integer | yes      | ID of the post       |
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: `type: object` (key-value pairs of integers)

4. **Create Post**
   - **Endpoint:** `POST /api/posts/create`
   - **Operation ID:** `createPost`
   - **Request Body (JSON):**
     ```json
     {
       "post": {
         "title": "string",
         "content": "string",
         "tags": ["string"],
         "image": "binary",
         "mysteryObject": { ... }
       }
     }
     ```
     Uses the [`PostCreationDto`](#postcreationdto) schema.
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: `type: object` (key-value pairs of integers)

5. **Search for Posts**
   - **Endpoint:** `GET /api/posts/searchForPosts`
   - **Operation ID:** `searchPosts`
   - **Parameters:**
     | Name     | In     | Type   | Required | Description                                       |
     |----------|--------|--------|----------|---------------------------------------------------|
     | q        | query  | string | yes      | Search query term                                 |
     | pageable | query  | object | yes      | Paging & sorting parameters (see [`Pageable`](#pageable)) |
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: [`PagedModel`](#pagedmodel)

6. **Get Posts for Post List**
   - **Endpoint:** `GET /api/posts/getForPostList`
   - **Operation ID:** `getPostsForPostList`
   - **Parameters:**
     | Name     | In     | Type   | Required | Description                                |
     |----------|--------|--------|----------|--------------------------------------------|
     | pageable | query  | object | yes      | Paging & sorting (see [`Pageable`](#pageable)) |
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: [`PagedModelPostListDto`](#pagedmodelpostlistdto)

7. **Get Post Details**
   - **Endpoint:** `GET /api/posts/getForPostDetails/{postId}`
   - **Operation ID:** `getPostDetails`
   - **Parameters:**
     | Name   | In   | Type    | Required | Description       |
     |--------|------|---------|----------|-------------------|
     | postId | path | integer | yes      | ID of the post    |
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: [`PostDetailsDto`](#postdetailsdto)

---

### Notification Controller

The **Notification Controller** deals with user notifications, including reading and retrieving notifications.

1. **Mark Notification as Read**
   - **Endpoint:** `PUT /api/notifications/{notificationId}/read`
   - **Operation ID:** `markAsRead`
   - **Parameters:**
     | Name           | In   | Type    | Required | Description              |
     |----------------|------|---------|----------|--------------------------|
     | notificationId | path | integer | yes      | ID of the notification   |
   - **Responses:**
     - **200 OK**

2. **Get User Notifications**
   - **Endpoint:** `GET /api/notifications/{userId}`
   - **Operation ID:** `getUserNotifications`
   - **Parameters:**
     | Name   | In   | Type    | Required | Description         |
     |--------|------|---------|----------|---------------------|
     | userId | path | integer | yes      | ID of the user      |
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: `array` of [`NotificationDto`](#notificationdto)

---

### Mystery Object Image Controller

1. **Upload Image**
   - **Endpoint:** `POST /api/mysteryObjects/{id}/upload-image`
   - **Operation ID:** `uploadImage`
   - **Parameters:**
     | Name | In   | Type    | Required | Description              |
     |------|------|---------|----------|--------------------------|
     | id   | path | integer | yes      | ID of the mystery object |
   - **Request Body (JSON):**
     ```json
     {
       "image": "binary"
     }
     ```
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: `type: object` (key-value pairs of strings)

---

### Comment Controller

The **Comment Controller** handles creating and voting on comments, as well as retrieving comments for a specific post.

1. **Upvote Comment**
   - **Endpoint:** `POST /api/comments/upvote/{commentId}`
   - **Operation ID:** `upvoteComment`
   - **Parameters:**
     | Name      | In   | Type    | Required | Description          |
     |-----------|------|---------|----------|----------------------|
     | commentId | path | integer | yes      | ID of the comment    |
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: `type: object` (key-value pairs of integers)

2. **Downvote Comment**
   - **Endpoint:** `POST /api/comments/downvote/{commentId}`
   - **Operation ID:** `downvoteComment`
   - **Parameters:**
     | Name      | In   | Type    | Required | Description          |
     |-----------|------|---------|----------|----------------------|
     | commentId | path | integer | yes      | ID of the comment    |
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: `type: object` (key-value pairs of integers)

3. **Create Comment**
   - **Endpoint:** `POST /api/comments/create`
   - **Operation ID:** `createComment`
   - **Request Body (JSON):** Uses [`CommentCreateDto`](#commentcreatedto)
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: `type: object` (key-value pairs of integers)

4. **Get Comments for Post**
   - **Endpoint:** `GET /api/comments/get/{postId}`
   - **Operation ID:** `getCommentsForPost`
   - **Parameters:**
     | Name   | In   | Type    | Required | Description       |
     |--------|------|---------|----------|-------------------|
     | postId | path | integer | yes      | ID of the post    |
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: `array` of [`CommentDetailsDto`](#commentdetailsdto)

---

### User Controller

The **User Controller** manages user registration, login, and retrieving user-specific data (profile, posts, comments).

1. **Register User**
   - **Endpoint:** `POST /api/auth/register`
   - **Operation ID:** `registerUser`
   - **Request Body (JSON):** [`User`](#user)
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: `type: object`

2. **Login User**
   - **Endpoint:** `POST /api/auth/login`
   - **Operation ID:** `loginUser`
   - **Request Body (JSON):** [`User`](#user)
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: `type: object`

3. **Get User Name**
   - **Endpoint:** `GET /api/auth/{userId}`
   - **Operation ID:** `getUserName`
   - **Parameters:**
     | Name   | In   | Type    | Required | Description       |
     |--------|------|---------|----------|-------------------|
     | userId | path | integer | yes      | ID of the user    |
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: `type: object` (key-value pairs of strings)

4. **Get User Posts**
   - **Endpoint:** `GET /api/auth/{userId}/posts`
   - **Operation ID:** `getUserPosts`
   - **Parameters:**
     | Name   | In   | Type    | Required | Description      |
     |--------|------|---------|----------|------------------|
     | userId | path | integer | yes      | ID of the user   |
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: `array` of [`PostListDto`](#postlistdto)

5. **Get User Comments**
   - **Endpoint:** `GET /api/auth/{userId}/comments`
   - **Operation ID:** `getUserComments`
   - **Parameters:**
     | Name   | In   | Type    | Required | Description      |
     |--------|------|---------|----------|------------------|
     | userId | path | integer | yes      | ID of the user   |
   - **Responses:**
     - **200 OK**  
       Content: `application/hal+json`  
       Schema: `array` of [`CommentDetailsDto`](#commentdetailsdto)

---

## Schemas (Data Models)

Below are the schemas referenced in the endpoints.

### PostCreationDto

~~~yaml
type: object
properties:
  title:
    type: string
  content:
    type: string
  tags:
    type: array
    items:
      type: string
    uniqueItems: true
  image:
    type: string
    format: binary
  mysteryObject:
    $ref: '#/components/schemas/MysteryObject'
~~~

### CommentCreateDto

~~~yaml
type: object
properties:
  content:
    type: string
  postId:
    type: integer
    format: int64
  parentCommentId:
    type: integer
    format: int64
~~~

### PostDetailsDto

~~~yaml
type: object
properties:
  id:
    type: integer
    format: int64
  author:
    type: string
  title:
    type: string
  description:
    type: string
  tags:
    type: array
    items:
      type: string
    uniqueItems: true
  mysteryObject:
    $ref: '#/components/schemas/MysteryObject'
  createdAt:
    type: string
    format: date-time
  updatedAt:
    type: string
    format: date-time
  upvotes:
    type: integer
    format: int32
  downvotes:
    type: integer
    format: int32
  userUpvoted:
    type: boolean
  userDownvoted:
    type: boolean
~~~

### CommentDetailsDto

~~~yaml
type: object
properties:
  id:
    type: integer
    format: int64
  content:
    type: string
  author:
    type: string
  createdAt:
    type: string
    format: date-time
  updatedAt:
    type: string
    format: date-time
  upvotes:
    type: integer
    format: int32
  downvotes:
    type: integer
    format: int32
  userUpvoted:
    type: boolean
  userDownvoted:
    type: boolean
  postId:
    type: integer
    format: int64
  bestAnswer:
    type: boolean
~~~

### MysteryObject

~~~yaml
type: object
properties:
  id:
    type: integer
    format: int64
  description:
    type: string
  writtenText:
    type: string
  color:
    type: string
  shape:
    type: string
  descriptionOfParts:
    type: string
  location:
    type: string
  hardness:
    type: string
  timePeriod:
    type: string
  smell:
    type: string
  taste:
    type: string
  texture:
    type: string
  value:
    type: number
    format: double
  originOfAcquisition:
    type: string
  pattern:
    type: string
  brand:
    type: string
  print:
    type: string
  functionality:
    type: string
  imageLicenses:
    type: string
  markings:
    type: string
  handmade:
    type: boolean
  oneOfAKind:
    type: boolean
  sizeX:
    type: number
    format: double
  sizeY:
    type: number
    format: double
  sizeZ:
    type: number
    format: double
  weight:
    type: number
    format: double
  item_condition:
    type: string
    enum:
      - NEW
      - LIKE_NEW
      - USED
      - DAMAGED
      - ANTIQUE
  image:
    type: string
    format: byte
~~~

### NotificationDto

~~~yaml
type: object
properties:
  id:
    type: integer
    format: int64
  message:
    type: string
  type:
    type: string
  postId:
    type: integer
    format: int64
  commentId:
    type: integer
    format: int64
  userId:
    type: integer
    format: int64
  read:
    type: boolean
~~~

### User

~~~yaml
type: object
properties:
  id:
    type: integer
    format: int64
  username:
    type: string
  email:
    type: string
  password:
    type: string
  bio:
    type: string
  profilePictureUrl:
    type: string
  createdAt:
    type: string
    format: date-time
  updatedAt:
    type: string
    format: date-time
  receiveNotifications:
    type: boolean
~~~

### PostListDto

~~~yaml
type: object
properties:
  id:
    type: integer
    format: int64
  title:
    type: string
  description:
    type: string
  tags:
    type: array
    items:
      type: string
    uniqueItems: true
  mysteryObjectImage:
    type: string
    format: byte
  solved:
    type: boolean
~~~

### Pageable

~~~yaml
type: object
properties:
  page:
    type: integer
    format: int32
    minimum: 0
  size:
    type: integer
    format: int32
    minimum: 1
  sort:
    type: array
    items:
      type: string
~~~

### PagedModel

~~~yaml
type: object
properties:
  content:
    type: array
    items:
      type: object
  page:
    $ref: '#/components/schemas/PageMetadata'
~~~

### PagedModelPostListDto

~~~yaml
type: object
properties:
  content:
    type: array
    items:
      $ref: '#/components/schemas/PostListDto'
  page:
    $ref: '#/components/schemas/PageMetadata'
~~~

### PageMetadata

~~~yaml
type: object
properties:
  size:
    type: integer
    format: int64
  totalElements:
    type: integer
    format: int64
  totalPages:
    type: integer
    format: int64
  number:
    type: integer
    format: int64
~~~

