from flask import Flask, render_template
import requests

app = Flask(__name__)
blog_data = requests.get("https://api.npoint.io/4b100e1730f8ba42eea1").json()


@app.route("/")
def home():

    return render_template("index.html", posts=blog_data)

@app.route("/about")
def about():
    return render_template("about.html")

@app.route("/contact")
def contact():
    return render_template("contact.html")

@app.route("/post/<int:index>")
def get_post(index):
    blog = blog_data[index-1]
    return render_template("post.html", post=blog)

if __name__ == "__main__":
    app.run(debug=True)