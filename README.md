# API for a search engine
An API written in Scala used to implement basic functionality of a search engine. Searches/Results are retained in the database and analytics are processed on-the-fly; all queries are processed by DuckDuckGo's [Instant Answer API](https://duckduckgo.com/api).


## Install instructions:
1. Docker and SBT are required to be installed and on your PATH.
  * If using Windows/Mac, the DOCKER_HOST env variable needs to be set, if it isn't already set:
    1. 
    ```docker-machine ip %MACHINE_NAME_HERE%```
    
    2. Set the env variable for the result of that command with:
    ```export DOCKER_HOST=XXX.XXX.XXX.XXX```

2. Database setup

  ```
  cd %PROJECT_ROOT%/db
  docker build -t search_db_img .
  docker run --name search_db -p 5432:5432 -d search_db_img
  ```

3. Run the API

  ```
  cd %PROJECT_ROOT%/api
  sbt run
  ```

## Endpoints
  * GET /most_common_search
    * Shows the most common search(es) globally.
  * GET /search_terms
    * Returns all (distinct) search terms ever searched for by all users.
    
  * POST /create_user
    * Example:
    
      ```
      curl -X POST -H 'Content-Type: application/json' -d '{"username":"brandon","password":"hunter2"}' http://localhost:8080/create_user
      ```
  * POST /search
    * Example:
    
      ```
      curl -H "Content-Type: application/json" -X POST -d '{"username":"brandon","password":"hunter2"}' http://localhost:8080/search?q=beer
      ```
  * POST /change_password
    * Example:
    
      ```
      curl -H "Content-Type: application/json" -X POST -d '{"username":"brandon","oldPassword":"hunter2", "newPassword": "betterpass"}' http://localhost:8080/change_password
      ```
  * POST /most_common_search
    * Finds most ocmmon search for an individual user.
    * Example:
    
      ```
      curl -H "Content-Type: application/json" -X POST -d '{"username":"brandon","password":"betterpass"}' http://localhost:8080/most_common_search
      ```

Created by Brandon Annin
