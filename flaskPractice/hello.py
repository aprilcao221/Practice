from flask import Flask
import random

app = Flask(__name__)


@app.route("/")
def hello_world():
    return "<p>Hello, World!</p>"

@app.route("/bye")
def say_bye():
    return "bye"

if __name__ == "__main__":
    app.run()


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