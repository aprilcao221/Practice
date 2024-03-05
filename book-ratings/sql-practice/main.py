from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy import Integer, String, Float

# import sqlite3
# db = sqlite3.connect("books-collection.db")
# cursor = db.cursor()
# cursor.execute("CREATE TABLE books("
#                "id INTEGER PRIMARY KEY, "
#                "title varchar(250) NOT NULL UNIQUE, "
#                "author varchar(250) NOT NULL, "
#                "rating FLOAT NOT NULL)"
#                )
# cursor.execute("INSERT INTO books VALUES(1, 'Harry Poter', 'J.K.Rowling', '9.3')")
# db.commit()

app = Flask(__name__)

## create database
class Base(DeclarativeBase):
    pass

app.config['SQLALCHEMY_DATABASE_URI'] = "sqlite:///new_books_collection.db"
# create the extension
db = SQLAlchemy(model_class=Base)
# initialize the app with the extension
db.init_app(app)

class Book(db.Model):
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    title: Mapped[str] = mapped_column(String(250), unique=True, nullable=False)
    author: Mapped[str] = mapped_column(String(250), nullable=False)
    rating: Mapped[float] = mapped_column(Float, nullable=False)

    # Optional: this will allow each book object to be identified by its title when printed.
    def __repr__(self):
        return f'<Book {self.title}>'



with app.app_context():
    ## Create table schema in the database. Requires application context.
    #     db.create_all()

    # # CREATE RECORD
    # new_book = Book(title="Harry Potter", author="Charles Dickens", rating=7.6)
    # db.session.add(new_book)
    # db.session.commit()

    ## Read all records
    # result = db.session.execute(db.select(Book).order_by(Book.title))
    # all_books = result.scalars()
    # book = result.scalar()
    # print(all_books)

    ## update a records by query
    # book_to_update = db.session.execute(db.select(Book).where(Book.title == "Harry Potter")).scalar()
    # book_to_update.title = "Harry Potter and the Chamber of Secrets"
    # db.session.commit()

    ## update a record by primary key
    # book_to_delete = db.session.execute(db.select(Book).where(Book.id == 1)).scalar()
    # or book_to_delete = db.get_or_404(Book, ident=2)
    # db.session.delete(book_to_delete)
    # db.session.commit()

    ## delete a particular record by primary key
    # book_to_delete = db.session.execute(db.select(Book).where(Book.id == 1)).scalar()
    book_to_delete = db.get_or_404(Book, 2)
    db.session.delete(book_to_delete)
    db.session.commit()


