-- src/main/resources/seed.sql
INSERT INTO USERS (EMAIL, PASSWORD_HASH, ACTIVE) VALUES ('admin@example.com', '$2a$12$examplehashedpassword', TRUE);
INSERT INTO USERS (EMAIL, PASSWORD_HASH, ACTIVE) VALUES ('user1@example.com', '$2a$12$examplehashedpassword', TRUE);

INSERT INTO ITEMS (TITLE, CATEGORY, TAGS, DESCRIPTION, IMAGE_URL, PRICE) VALUES ('Item A', 'Category1', 'tag1,tag2', 'Description A', 'http://example.com/a.png', 10.00);
INSERT INTO ITEMS (TITLE, CATEGORY, TAGS, DESCRIPTION, IMAGE_URL, PRICE) VALUES ('Item B', 'Category2', 'tag3,tag4', 'Description B', 'http://example.com/b.png', 20.00);
