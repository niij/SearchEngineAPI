CREATE TABLE Users(id serial PRIMARY KEY, username TEXT NOT NULL UNIQUE, password TEXT NOT NULL);
CREATE TABLE Searches(id serial PRIMARY KEY, userid Int NOT NULL, searchquery TEXT NOT NULL);
CREATE TABLE Results(id serial PRIMARY KEY, searchid Int NOT NULL, description TEXT NOT NULL);
