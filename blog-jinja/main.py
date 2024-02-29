from flask import Flask, render_template
from post import Post
import requests
URL = "https://api.npoint.io/e66155c209e22dcb2644"

posts = requests.get(URL).json()
post_objs = []
for post in posts:
    post_obj = Post(post["id"], post["title"], post["subtitle"], post["body"])
    post_objs.append(post_obj)

app = Flask(__name__)

@app.route('/')
def home():
    return render_template("index.html", posts=post_objs)
@app.route('/post/<int:index>')
def get_post(index):
    requested_post = None
    for blog in post_objs:
        if blog.id == index:
            requested_post = blog
    return render_template("post.html", post=requested_post)



if __name__ == "__main__":
    app.run(debug=True)
