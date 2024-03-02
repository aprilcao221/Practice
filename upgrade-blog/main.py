from flask import Flask, render_template, request
import requests
import smtplib
import os

app = Flask(__name__)
blog_data = requests.get("https://api.npoint.io/4b100e1730f8ba42eea1").json()
psw = os.environ["psw"]


@app.route("/")
def home():

    return render_template("index.html", posts=blog_data)

@app.route("/about", methods=["POST", "GET"])
def about():
        return render_template("about.html")

@app.route("/contact", methods=["POST", "GET"])
def contact():
    if request.method == "POST":
        form_data = request.form
        with smtplib.SMTP("smtp.163.com", 25) as connection:
            connection.starttls()
            connection.login("caomancomeon@163.com", psw)
            connection.sendmail(from_addr="caomancomeon@163.com",
                                to_addrs="caomancomeon@gmail.com",
                                msg=f"subject: New contact\n\n You've got a new contact from {form_data['name']}\n"
                                    f"Here's the contact info:\n"
                                    f"phone number: {form_data['phone']}\n"
                                    f"email: {form_data['email']}\n"
                                    f"Here's the message:"
                                    f"{form_data['message']}")
        return render_template("contact.html", received=True)
    return render_template("contact.html")

@app.route("/post/<int:index>")
def get_post(index):
    blog = blog_data[index-1]
    return render_template("post.html", post=blog)


if __name__ == "__main__":
    app.run(debug=True)