from flask import Flask


app = Flask(__name__)

def make_bold(function):
    def wrapper_function():
        text = function()
        return f"<b>{text}</b>"
    return wrapper_function

def make_emphasis(function):
    def wrapper_function():
        text = function()
        return f"<em>{text}</em>"
    return wrapper_function

def make_underline(function):
    def wrapper_function():
        text = function()
        return f"<u>{text}</u>"
    return wrapper_function

@app.route("/")
def hello_world():
    return ('<h1 style="text-align: center">Hello, World!</h1>'
            '<p>This is a paragrabh</p>'
            '<img src="https://media.giphy.com/media/phJ6eMRFYI6CQ/giphy.gif?cid=790b7611jds0r8ycklemmxw1lau8wa5vj2w8k32cv7czs7d5&ep=v1_gifs_search&rid=giphy.gif&ct=g" '
            'width=200>'
            )

@app.route("/bye")
@make_bold
@make_emphasis
@make_underline
def say_bye():
    return "bye"

@app.route("/<path:name>/<int:number>")
def greet(name, number):
    return f"Hello there {name}, you are {number} year old!"

if __name__ == "__main__":
    app.run(debug=True)


# def add(n1, n2):
#     return n1 + n2
#
# def subtract(n1, n2):
#     return n1 - n2
#
# def multiply(n1, n2):
#     return n1 * n2
#
# def divide(n1, n2):
#     return n1 / n2
# def calculate(calc_function, n1, n2):
#     return calc_function(n1, n2)
#
# result = calculate(add, 2, 3)
# print(result)
#
# def outer_function():
#     print("I'm outer")
#     def nested_fuction():
#         print("I'm inner")

# def outer_function():
#     print("I'm outer")
#     def nested_fuction():
#         print("I'm inner")
#     return nested_fuction
#
#
# inner_function = outer_function()
# inner_function()

# import time
# def delay_decorator(function):
#     def wrapper_function():
#         time.sleep(2)
#         function()
#         function()
#     return wrapper_function
#
# @delay_decorator
# def say_hello():
#     print("hello")
#
# def say_bye():
#     print("bye")
#
# def say_greeting():
#     print("how are you")
#
# decorated_function = delay_decorator(say_bye)
# decorated_function()