import turtle
from turtle import Turtle

class ScoreBoard(Turtle):
    def __init__(self, x_pos):
        super().__init__()
        self.score = 0
        self.color("white")
        self.hideturtle()
        self.penup()
        self.goto(x_pos, 260)
        self.update_scoreboard()

    def update_scoreboard(self):
        self.clear()
        self.write(f"{self.score}", False, "center", ("courier", 40, "normal"))

    def increase_score(self):
        self.score += 1
        self.update_scoreboard()

