from flask import Flask, render_template
from flask_bootstrap import Bootstrap5
import csv
from flask_wtf import FlaskForm
from wtforms import StringField, SubmitField, SelectField
from wtforms.validators import DataRequired, URL

class AddForm(FlaskForm):
    name = StringField('Cafe Name', validators=[DataRequired()])
    location = StringField(label='Cafe Location on Google Map(URL)', validators=[DataRequired(), URL()])
    open_time = StringField(label='Opening time e.g. 8AM', validators=[DataRequired()])
    close_time = StringField(label='Closing time e.g. 5:30PM', validators=[DataRequired()])
    coffe_rating = SelectField(label='Coffee rating', choices=[("bad", "❌"), ("poor", "☕️"), ("normal", "☕️☕️"), ("gret", "☕️☕️️☕️")])
    wifi_rating = SelectField(label="Wifi rating", choices=[("bad", "❌"), ("poor", "📶"), ("normal", "📶📶"), ("gret", "📶📶")])
    power_rating = SelectField(label="Power rating", choices=[("bad", "❌"), ("poor", "🔌"), ("normal", "🔌🔌"), ("gret", "🔌🔌🔌")])
    submit = SubmitField("Submit")

app = Flask(__name__)
bootstrap = Bootstrap5(app)
app.secret_key = "this-is-a-secret"

@app.route("/")
def home():
    return render_template("home.html")

@app.route("/cafes")
def get_cafes():
    with open("cafe-data.csv") as file:
        cafe_data = file.readlines()
        cafes_data = [row.rstrip().split(",") for row in cafe_data][1:]
    return render_template("cafes.html", cafes=cafes_data)

@app.route("/add", methods=["POST", "GET"])
def add_data():
    add_form = AddForm()
    if add_form.validate_on_submit():
        with open("cafe-data.csv", "a") as file:
            file.write(f"\n{add_form.name.data},"
                       f"{add_form.location.data},"
                       f"{add_form.open_time.data},"
                       f"{add_form.close_time.data},"
                       f"{add_form.coffe_rating.data},"
                       f"{add_form.wifi_rating.data},"
                       f"{add_form.power_rating.data}")
    return render_template("add.html", form=add_form)

if __name__ == "__main__":
    app.run(debug=True)