from flask import Flask, render_template, redirect, url_for, request
from flask_bootstrap import Bootstrap5
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy import Integer, String, Float
from flask_wtf import FlaskForm
from wtforms import StringField, SubmitField, FloatField, IntegerField
from wtforms.validators import DataRequired
import requests

'''
Red underlines? Install the required packages first: 
Open the Terminal in PyCharm (bottom left). 

On Windows type:
python -m pip install -r requirements.txt

On MacOS type:
pip3 install -r requirements.txt

This will install the packages from requirements.txt for this project.
'''


movie_search_url = "https://api.themoviedb.org/3/search/movie"
headers = {
            "accept": "application/json",
            "Authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJkOGFkNzEzYzg1OTMyOWZiYmFiNmRlOTRjYzNlYWEwNiIsInN1YiI6IjY1ZTcwZTU2ZWY4YjMyMDE2MmQ3NjM3OSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.X0jXA09mDPLI7NiJq0J2Vwef37oddpTa3QBbtM1E0fo"
        }
app = Flask(__name__)
app.config['SECRET_KEY'] = '8BYkEfBA6O6donzWlSihBXox7C0sKR6b'
Bootstrap5(app)

# CREATE DB
class Base(DeclarativeBase):
    pass
app.config['SQLALCHEMY_DATABASE_URI'] = "sqlite:///movies.db"
db = SQLAlchemy(model_class=Base)
db.init_app(app)

class EditForm(FlaskForm):
    rating = FloatField(label="Your rating out of 10.0", validators=[DataRequired()])
    ranking = IntegerField(label="Your ranking from 1 to 10", validators=[DataRequired()])
    review = StringField(label="Your review", validators=[DataRequired()])
    submit = SubmitField(label="Done")

class AddForm(FlaskForm):
    movie = StringField(label="Movie Title", validators=[DataRequired()])
    submit = SubmitField(label="Add Movie")

# CREATE TABLE
class Movie(db.Model):
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    title: Mapped[str] = mapped_column(String, unique=True, nullable=False)
    year: Mapped[int] = mapped_column(Integer, nullable=True)
    description: Mapped[str] = mapped_column(String, nullable=True)
    rating: Mapped[float] = mapped_column(Float, nullable=True)
    ranking: Mapped[int] = mapped_column(Integer, nullable=True)
    review: Mapped[str] = mapped_column(String, nullable=True)
    img_url: Mapped[str] = mapped_column(String, nullable=True)

# with app.app_context():
#     db.create_all()

@app.route("/")
def home():
    with app.app_context():
        all_movies = db.session.execute(db.select(Movie).order_by(Movie.ranking)).scalars()
        return render_template("index.html", movies=all_movies)


@app.route("/edit/<int:num>", methods=["POST", "GET"])
def edit(num):
    edit_form = EditForm()
    if request.method == "POST":
        if edit_form.validate_on_submit():
            with app.app_context():
                selected_movie = db.session.execute(db.select(Movie).where(Movie.id == num)).scalar()
                selected_movie.rating = edit_form.rating.data
                selected_movie.ranking = edit_form.ranking.data
                selected_movie.review = edit_form.review.data
                db.session.commit()
            return redirect(url_for('home'))
    return render_template("edit.html", form=edit_form)


@app.route("/delete/<int:num>")
def delete(num):
    with app.app_context():
        selected_movie = db.get_or_404(Movie, ident=num)
        db.session.delete(selected_movie)
        db.session.commit()
    return redirect(url_for('home'))

@app.route("/add", methods=["POST", "GET"])
def add():
    add_form = AddForm()
    if add_form.validate_on_submit():
        movie_title = add_form.movie.data
        response = requests.get(movie_search_url, headers=headers, params={'query': movie_title}).json()
        options = response["results"]
        return render_template("select.html", movies=options)
    return render_template("add.html", form=add_form)

@app.route("/find")
def find():
    movie_id = request.args.get("movie_id")
    if movie_id:
        response = requests.get(f"https://api.themoviedb.org/3/movie/{movie_id}?language=en-US", headers=headers).json()
        new_movie = Movie(title=response['title'],
                          year=response['release_date'].split("-")[0],
                          description=response['overview'],
                          img_url=f"https://image.tmdb.org/t/p/w500/{response['poster_path']}")
        with app.app_context():
            db.session.add(new_movie)
            db.session.commit()
    return redirect(url_for('home'))


if __name__ == '__main__':
    app.run(debug=True)
