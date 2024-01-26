import turtle as t
from turtle import Screen, Turtle
from random import choice
screen = Screen()
t.colormode(255)
timmy = Turtle()
color_list = [(209, 155, 95), (188, 62, 30), (142, 144, 156), (226, 213, 108), (234, 217, 225), (101, 103, 133), (207, 147, 177), (177, 157, 44), (224, 233, 226), (98, 107, 178), (28, 27, 68), (30, 47, 28), (37, 40, 19), (193, 20, 7), (226, 168, 198), (44, 45, 103), (211, 86, 59), (129, 86, 96), (235, 173, 159), (89, 101, 91), (156, 164, 157), (206, 80, 107), (182, 184, 213), (181, 16, 22), (44, 27, 45), (70, 71, 41), (223, 204, 25), (52, 71, 54)]

timmy.penup()
timmy.setposition(-200, -200)


def draw_dot_horizontal():
    x = -200
    for i in range(10):
        timmy.pendown()
        timmy.dot(20, choice(color_list))
        x += 50
        timmy.penup()
        timmy.setx(x)
    x = -200
    timmy.setx(-200)
y = -200
for i in range(10):
    y += 50
    draw_dot_horizontal()
    timmy.sety(y)




screen.exitonclick()