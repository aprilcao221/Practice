from flask import Flask, render_template, request, redirect, url_for
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy import Integer, Float, String



'''
Red underlines? Install the required packages first: 
Open the Terminal in PyCharm (bottom left). 

On Windows type:
python -m pip install -r requirements.txt

On MacOS type:
pip3 install -r requirements.txt

This will install the packages from requirements.txt for this project.
'''

app = Flask(__name__)

class Base(DeclarativeBase):
    pass

app.config['SQLALCHEMY_DATABASE_URI'] = "sqlite:///books.db"
db = SQLAlchemy(model_class=Base)
db.init_app(app)

class Book(db.Model):
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    title: Mapped[str] = mapped_column(String, unique=True, nullable=False)
    author: Mapped[str] = mapped_column(String, nullable=False)
    rating: Mapped[float] = mapped_column(Float, nullable=False)

# with app.app_context():
#     db.create_all()


@app.route('/')
def home():
    all_books = db.session.execute(db.select(Book)).scalars()
    return render_template("index.html", books=all_books)


@app.route("/add", methods=["GET", "POST"])
def add():
    add_success = False
    if request.method == "POST":
        new_book = Book(
            title=request.form["title"],
            author=request.form["author"],
            rating=request.form["rating"],
        )
        db.session.add(new_book)
        db.session.commit()
        add_success = True
        return redirect(url_for('home'))
    return render_template("add.html", value=add_success)

@app.route("/edit/<int:id>", methods=["POST", "GET"])
def change_rating(id):
    book_to_update = db.session.execute(db.select(Book).where(Book.id == id)).scalar()
    if request.method == "POST":
        book_to_update.rating = request.form["rating"]
        db.session.commit()
        return redirect(url_for('home'))
    return render_template("update.html", book=book_to_update)

@app.route("/delete/<int:id>")
def delete_book(id):
    book_to_delete = db.session.execute(db.select(Book).where(Book.id == id)).scalar()
    db.session.delete(book_to_delete)
    db.session.commit()
    return redirect(url_for('home'))





if __name__ == "__main__":
    app.run(debug=True)

