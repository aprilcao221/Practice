from flask import Flask, render_template
import requests

app = Flask(__name__)


@app.route("/")
def home():
    return render_template("home.html")

@app.route("/blog/<number>")
def get_blog(number):
    blog_response = requests.get(url="https://api.npoint.io/e66155c209e22dcb2644")
    all_posts = blog_response.json()
    return render_template("blog.html", posts=all_posts)


if __name__ == "__main__":
    app.run(debug=True)