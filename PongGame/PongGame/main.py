from turtle import Screen, Turtle
from paddle import Paddle
from ball import Ball
import time
from scoreboard import ScoreBoard


screen = Screen()
screen.tracer(0)
ball = Ball()
r_scoreboard = ScoreBoard(-60)
l_scoreboard = ScoreBoard(60)

screen.setup(800, 600)
screen.title("Pong")
screen.bgcolor("black")
screen.listen()

r_paddle = Paddle(350)
l_paddle = Paddle(-350)

game_on = True

while game_on:
    time.sleep(ball.move_speed)
    screen.update()
    screen.onkey(r_paddle.go_up, "Up")
    screen.onkey(r_paddle.go_down, "Down")
    screen.onkey(l_paddle.go_up, "w")
    screen.onkey(l_paddle.go_down, "s")
    ball.move()

    if ball.ycor() > 280 or ball.ycor() < -280:
        ball.bounce_y()
    if ball.distance(r_paddle) < 50 and ball.xcor() > 320:
        ball.bounce_x()
        r_scoreboard.increase_score()
    if ball.distance(l_paddle) < 50 and ball.xcor() < -320:
        ball.bounce_x()
        l_scoreboard.increase_score()
    if ball.xcor() > 380:
        ball.reset_position()
    if ball.xcor() < -380:
        ball.reset_position()


screen.exitonclick()