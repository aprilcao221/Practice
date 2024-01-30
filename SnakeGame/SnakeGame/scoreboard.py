from turtle import Turtle
ALIGNMENT = "center"
FONT = ('Courier', 20, 'normal')

class ScoreBoard(Turtle):
    def __init__(self):
        super().__init__()
        self.score = 0
        self.pencolor("white")
        self.penup()
        self.hideturtle()
        self.goto(0, 270)
        with open("data.txt") as data:
            self.highscore = int(data.read())
        self.update_score()

    def update_score(self):
        self.clear()
        self.write(f"Score: {self.score} Highest Score: {self.highscore}", False, ALIGNMENT, FONT)

    def reset(self):
        if self.score > self.highscore:
            self.highscore = self.score
            with open("data.txt", mode="w") as data:
                data.write(f"{self.highscore}")
        self.score = 0
        self.update_score()

    def increase_score(self):
        self.score += 1
        self.update_score()
