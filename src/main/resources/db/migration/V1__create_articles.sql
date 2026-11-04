CREATE TABLE articles (
    id INTEGER PRIMARY KEY,
    titulo VARCHAR(500) NOT NULL,
    abstract_text TEXT NOT NULL
);

CREATE TABLE corpus_import_lock (
    id INTEGER PRIMARY KEY
);

INSERT INTO corpus_import_lock (id) VALUES (1);
