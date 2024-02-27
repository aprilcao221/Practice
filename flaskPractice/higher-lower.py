from flask import Flask
from random import randint
app = Flask(__name__)
high_num = 9
right_number = randint(0, high_num)
print(right_number)


@app.route("/")
def homepage():
    return (f'<h1>Guess a number between 0 and {high_num}</h1>'
            '<img src="https://media.giphy.com/media/l378khQxt68syiWJy/giphy.gif?cid=790b7611mngx20zpoo83i4yl705f6x9hvucuqa76v6i3btix&ep=v1_gifs_search&rid=giphy.gif&ct=g"'
            'width=500>')


@app.route("/<int:num>")
def user_num(num):
    if num > right_number:
        return ('<h1 style="color: red">Too high, try again!</h1>'
                '<img src="https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExNWY0amNoaGEydHY2NHdweXI2NnBmcjR3NjUyaDR0MTlqODZ0YTVmNCZlcD12MV9naWZzX3NlYXJjaCZjdD1n/OPU6wzx8JrHna/giphy.gif"'
                'width=500>')
    if num < right_number:
        return ('<h1 style="color: blue">Too low, try again!</h1>'
                '<img src="https://media.giphy.com/media/J1XSaMzkdlqDl89NVf/giphy.gif?cid=ecf05e47q46h683de0ick9n4sllrwwnjyx5q24g4eq4bju2r&ep=v1_gifs_search&rid=giphy.gif&ct=g"'
                'width=500>')
    if num == right_number:
        return ('<h1 style="color: green">You found me!</h1>'
                '<img src="https://media.giphy.com/media/rdma0nDFZMR32/giphy.gif?cid=ecf05e47u3l1c32zshmqdbcyrxiyip8peq2eogs29am785qk&ep=v1_gifs_search&rid=giphy.gif&ct=g"'
                'width=500>')


if __name__ == "__main__":
    app.run(debug=True)
    