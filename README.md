# Stardao

Simple immutable-friendly base DAO classes for MongoDB and DynamoDB

## Why use this?

These Data Access Object (DAO) superclasses are a good choice if:

  - You want clean separation between your business logic and your DAO, mapping data to POJOs
  - *But* within your DAOs, you want to hand-code your data access, and don't want a heavy abstraction layer adding indirection between you and the full features of the database
  - **But** you don't want to have to hand-roll basic CRUD operations for every new DAO

You also might like these DAO superclasses if:

  - You favor immutable data model POJOs, such as those generated by AutoValue
  - You are switching back and forth between MongoDB and DynamoDB and want a common idiom
  - You're using Dropwizard, or something else that uses Jersey and Jackson
  - You want a good way to handle partial updates (PATCH requests) coming from users
  - You want to easily set the user id and timestamp on creation and update

## How To Use Stardao

### Installation

To use Stardao, add the following to your project's POM file:

```
<properties>
    <stardao.version>0.3.0</stardao.version>
    ...
</properties>

<dependencies>
  <dependency>
    <groupId>io.stardog.stardao</groupId>
    <artifactId>stardao-core</artifactId>
    <version>${stardao.version}</version>
  </dependency>
  ...
</dependencies>
```

Then add any of the following modules you want to use. If you are using DynamoDB:

```
  <dependency>
    <groupId>io.stardog.stardao</groupId>
    <artifactId>stardao-dynamodb</artifactId>
    <version>${stardao.version}</version>
  </dependency>
```

If you're using MongoDB:

```
  <dependency>
    <groupId>io.stardog.stardao</groupId>
    <artifactId>stardao-dynamodb</artifactId>
    <version>${stardao.version}</version>
  </dependency>
```

If you want to use the `@AutoPartial` feature for automatically generating Partial entity classes:

```
  <dependency>
    <groupId>io.stardog.stardao</groupId>
    <artifactId>stardao-auto</artifactId>
    <version>${stardao.version}</version>
  </dependency>
```

If you're using Jersey:

```
  <dependency>
    <groupId>io.stardog.stardao</groupId>
    <artifactId>stardao-jersey</artifactId>
    <version>${stardao.version}</version>
  </dependency>
```

### Define POJOs

Define your basic POJO entity model classes however you like. They can be immutable or not, as you prefer. The classes must be capable of being de/serialized with [Jackson](https://github.com/FasterXML/jackson), and they must use getters for field access.

The classes can be subclasses, but they should not derive fields from parents (the class hierarchy will not be examined for annotations).

Personally, the author prefers using [AutoValue](https://github.com/google/auto/blob/master/value/userguide/index.md) with builders, but it's up to you.

### Annotations

Place your annotations over the getter methods. All annotations are optional, but you should normally use `@Id`. If `@Id` is not present, but a `getId()` method is present, Stardao will assume that that's the id.

- ``@Id`` - mark the primary key
- ``@CreatedAt`` - field will be timestamped on creation
- ``@CreatedBy`` - field will be marked with the id of user who created it
- ``@UpdatedAt`` - field will be timestamped on update
- ``@UpdatedBy`` - field will be marked with the id of the user who updated
- ``@Updatable`` - marks a field as permissible to change in a user-generated partial update. For example, for a user, `email` might take updates, but `lastLoginTime` might not
- ``@Creatable`` - marks a field as permissible to set on creation, but not on an update
- ``@StorageName("name")`` - store a field under a different name in the database

You are also encouraged to use Hibernate Validator annotations, such as `@Email` or `@Min`.

### Use @AutoPartial to generate partials

While it's not required, you can set up your Dao to make a distinction between the base entity model class, and a supporting model class called a `Partial`. Partials are like entities, except every single field on them is an `Optional`. For example, if you have a `User` class, you might have a `PartialUser` to go with it.

This frees you to make all your required fields non-Nullable and non-Optional, on the base entity class. When you pass around and interact with a `User`, you can be certain that none of the required fields are null, and set up the non-required fields as `Optional`s.

So what are Partials useful for?
  - **Updates:** when performing partial updates, you usually only want to pass in the specific fields you intend to update. This idea is much better represented by a Partial than an entity.
  - **Creates:** when creating an entity, usually the caller omits the primary key id (letting the database generate the id). However, the id must exist on all of your entities after they're created. The same is usually true of created-by/created-at fields. So as long as it gets validated to ensure it's not missing any required fields, a Partial is a better way to pass in the initial data.
  - **Limited views:** sometimes you want to expose a view of an object, for a particular user, that has certain sensitive fields omitted. Or for performance/memory reasons, you only want to retrieve a couple of the fields from the database. Partials are a good way to represent these views.
  
Partials can't derive from the base class, since the methods have different signatures. So while you could write Partials yourself, you'd be violating DRY pretty badly. Fortunately, Stardao has a solution, inspired by Google's AutoValue. To automatically generate Partials, just include the `stardao-auto` module, and add `@AutoPartial` to your entity class.

Stardao will automatically generate a `PartialEntityname` class to go with your entity. This leverages AutoValue and will therefore be immutable. It uses builders, with no prefix on the setter methods. You can also convert a regular entity into a partial with `PartialEntityname.of(entity)`.

### Define Your Dao

Extend either the `AbstractMongoDao` or `AbstractDynamoDao` superclass. The type parameters are, in order:

- `M`: Model Class
- `P`: Partial Model Class (can be the same as the Model Class, if you don't want to use partials)
- `K`: Primary key type (type of the `@Id` field)
- `I`: User-id foreign key type (type of the `@CreatedBy` and `@UpdatedBy` fields)

For example, if you have a MongoDB collection called `org` to track organizations, that uses `Org` as the model class, `PartialOrg` as the partial model class, `Long `as the org `_id` and `ObjectId` as the associated user id type:

```
public class OrgDao extends AbstractMongoDao<Org,PartialOrg,Long,ObjectId> {
```

### Write a constructor

Typically the constructor takes the appropriate connection object from the driver and calls the superclass. For example, for MongoDB:

```
public class OrgDao extends AbstractMongoDao<Org,PartialOrg,Long,ObjectId> {
    public OrgDao(MongoDatabase db) {
        super(Org.class, PartialOrg.class, db.getCollection("org"));
    }
```

For DynamoDB:

```
public class OrgDao extends AbstractDynamoDao<Org,PartialOrg,Long,UUID> {
    public OrgDao(DynamoDB db) {
        super(Org.class, PartialOrg.class, db, "org");
    }
```

### What you get for free

Every Dao automatically comes with the following public methods:

- ``M load(K id)`` - loads an object and throws a DataNotFoundException (a runtime exception) if it's not found
- ``Optional<M> loadOpt(K id)`` - loads an object as an Optional
- ``M create(P partial, [I createdBy])`` - creates a new object
- ``void update(K id, Update<P> update[, I updatedBy])`` - perform a partial update
- ``M updateAndReturn(K id, Update<P> update[, I updatedBy])`` - perform an update and return the object prior to modification
- ``void delete(K id)`` - delete an object by id
- ``Iterable<M> iterateAll()`` - iterate through the whole table
- ``initTable()`` - initialize the table and ensure indexes (never destructive of data)
- ``dropTable()`` - drop the whole table

### Write your methods

With the boring CRUD out of the way, write methods specific to the model -- for example, you might use the conventions:

- ``loadByX`` queries that return an object by fields other than the id
- ``findByX`` queries that return ``Results<M,K>`` objects
- ``updateX`` methods that perform various updates
- Anything else you want -- the above are just suggestions

The two superclasses (`AbstractDynamoDao` and `AbstractMongoDao`) have some different protected methods that you can use to simplify writing queries.

Both superclasses have a mapper for transforming the model classes into the specific document type for the database (an ItemMapper for DynamoDB and a DocumentMapper for Mongo) which you can get at with a call to `getModelMapper()` and `getPartialMapper()`. You can convert the database-returned objects to your POJO with `getModelMapper().toObject(databaseObject)`

### Where's save()?

In the author's opinion, a save() (which typically overwrites the whole object), is fairly dangerous, and should be avoided in favor of partial updates, which are more performant and avoid potential race conditions.

Of course, if you want to add a save() to your Dao subclasses, nothing's stopping you from writing one.

### What about arbitrary queries?

Again purely in the author's opinion, all queries should be hand-written and live within the DAO. A codebase that allows any caller throughout the application to make any arbitrary queries -- especially arbitrary queries whose impact on the underlying storage layer are not fully understood -- is a dangerous codebase.

The author is also not fond of layering a special query syntax or DSL on top of the native driver. These tend to just add indirection, and the native syntax is usually just as easy to learn.

So you are advised to simply write findX methods on your subclass, using the specific functionality of the native drivers.

## Validation

The ``ModelValidator`` class provides a convenient shortcut to Hibernate Validation that allows you to easily validate your models, especially as passed in from JAX-RS endpoints.

- `validateModel()` validates every field present on the model, without regard for which ones are optional, updatable, or creatable
- `validateCreate()` validates the fields present on a model, ensuring that it is a valid user-submitted creation. Specifically that:
  - only `@Updatable` or `@Creatable` fields are present
  - all non-optional `@Updatable` and `@Creatable` fields are present
  - all present fields pass validation
- `validateUpdate()` validates an `Update`, ensuring that:
  - the `Update` is only touching `@Updatable` fields
  - the `Update` is not unsetting any non-optional fields
  - all fields being set pass validation

In each case the validate method will throw a runtime `DataValidationException` if it fails, and return true if it succeeds.

You should probably inject a `ModelValidator` instance, but for convenience, there is a static `DefaultModelValidator` that implements the methods.

## Using with Jersey / Dropwizard: Partial Updates

The `Update<P>` object comes with a Jackson deserializer, so you can include it as the body for PATCH requests.

A simple update resource method might look like:

```java
@PATCH
@Path("/org/{id}")
public Response updateOrg(@Auth User user, @PathParam("id") ObjectId id, Update<PartialOrg> update) {
    DefaultValidator.validateUpdate(update, orgDao);
    // perform ACL checks here...
    orgDao.update(id, update, user.getId());
    return Response.noContent().build();
}
```

If a user PATCHes a request like this:

```
/v1/org/123
{"name":"Stardog Ventures","email":null}
```

It will be interpreted as a request to set the `name` field to `Stardog Ventures`, and unset the `email `field. Other fields will not be changed. The whole request will only be allowed if both name and email are marked `@Updatable`.

Empty strings also will be treated as a request to unset fields.

## Using with Jersey / Dropwizard: Exception Mapper

You probably want to register the Stardao-specific exception modelMapper classes with Jersey. `DataValidationExceptionMapper` in particular provides a friendly 400 which contains all the errors for extraction.

```java
env.jersey().register(new DataNotFoundExceptionMapper());
env.jersey().register(new DataValidationExceptionMapper());
```
