[![CircleCI](https://circleci.com/gh/istonikula/realworld-api.svg?style=svg)](https://circleci.com/gh/istonikula/realworld-api)

This is a [realworld](http://realworld.io) api example app featuring 
[Arrow](http://arrow-kt.io), [Kotlin](http://kotlinlang.org) and [Spring Boot](http://spring.io/projects/spring-boot). 

# Goals
* experiment __functional__ Kotlin __backend__ programming using [Arrow](http://arrow-kt.io)
* implement [realworld](http://realworld.io) api 
[spec](http://github.com/gothinkster/realworld/tree/master/api) in order to cover non-trivial use cases
* production grade [rest api](http://en.wikipedia.org/wiki/Representational_state_transfer) with strong integration 
test suite to enable easy refactoring
* clear separation of core business logic (`realworld-domain`) from the supporting infrastructure 
(`realworld-app`, `realworld-infra`)
  * framework independent domain
  * adopted ideas from 
  [onion](https://bit.ly/2LqFhSz), 
  [hexagonal](https://bit.ly/2OBnVIo) and 
  [clean](https://bit.ly/2PKGYwk) architectures
  ([they might be all the same](https://bit.ly/2ItpiT7))
  * NOTE: `realworld-app` could be a submodule of `realword-infra`, but I wanted to put emphasis on the deliverables 
  by using a separate top level module 

# Prior art
* [Simple Inversion of Control in Kotlin without Dependency Injection Frameworks](https://bit.ly/2q2ccUg) 
  | [Part 2](https://bit.ly/2PJkn3d)

