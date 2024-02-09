from tkinter import *
from quiz_brain import QuizBrain

THEME_COLOR = "#375362"


class QuizInterface(QuizBrain):


    def __init__(self, q_list):
        super().__init__(q_list)
        self.window = Tk()
        self.window.title("Quizzler")
        self.window.config(padx=20, pady=20, bg=THEME_COLOR)
        self.score_label = Label(text="Score: 0", fg="white", bg=THEME_COLOR)
        self.score_label.grid(column=1, row=0)
        self.canvas = Canvas(width=300, height=250, bg="white")
        self.canvas_text = self.canvas.create_text(150, 125, text="", font=("Arial", 20, "italic"), width=280, fill=THEME_COLOR)
        self.canvas.grid(column=0, row=1, columnspan=2, pady=50)
        true_image = PhotoImage(file="images/true.png")
        false_image = PhotoImage(file="images/false.png")
        self.true_button = Button(image=true_image, highlightthickness=0, command=self.answer_true)
        self.false_button = Button(image=false_image, highlightthickness=0, command=self.answer_false)
        self.true_button.grid(column=0, row=2)
        self.false_button.grid(column=1, row=2)
        self.next_quiz()
        self.window.mainloop()

    def next_quiz(self):
        self.canvas.config(bg="white")
        if self.still_has_questions():
            self.canvas.itemconfig(self.canvas_text, text=self.next_question())
            self.score_label.config(text=f"Score: {self.score}")
        else:
            self.canvas.itemconfig(self.canvas_text, text=f"You've finished the quiz with score of {self.score}")
            self.true_button.config(state="disabled")
            self.false_button.config(state="disabled")

    def answer_true(self):
        self.give_feedback(self.check_answer("True"))

    def answer_false(self):
        self.give_feedback(self.check_answer("False"))

    def give_feedback(self, is_right):
        if is_right:
            self.canvas.config(bg="green")
        else:
            self.canvas.config(bg="red")
        self.window.after(1000, func=self.next_quiz)




