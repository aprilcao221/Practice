from flask import Flask, render_template
from random import randint
import datetime
import requests

app = Flask(__name__)


@app.route("/")
def home():
    # random_num = randint(1, 10)
    # current_year = datetime.date.today().year
    # my_name = "April"
    # return render_template("index.html", num=random_num, year=current_year, name=my_name)
    return "<h1>Welcome, please add your name to the route!</h1>"


@app.route("/guess/<name>")
def guess(name):
    params = {
        'name': name
    }
    age_response = requests.get(url="https://api.agify.io", params=params)
    age = age_response.json()['age']
    gender_response = requests.get(url="https://api.genderize.io", params=params)
    gender = gender_response.json()['gender']
    return render_template("index.html", name=name.title(), age=age, gender=gender)



if __name__ == "__main__":
    app.run(debug=True)

