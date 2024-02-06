from tkinter import *
from random import choice
import pandas
BACKGROUND_COLOR = "#B1DDC6"
current_word = {}
to_learn = []
try:
    data = pandas.read_csv("data/to_learn.csv")
except FileNotFoundError:
    original_data = pandas.read_csv("data/french_words.csv")
    to_learn = original_data.to_dict(orient="records")
else:
    to_learn = data.to_dict(orient="records")


def new_word():
    global flip_timer, current_word, to_learn
    window.after_cancel(flip_timer)
    current_word = choice(to_learn)
    canvas.itemconfig(title_text, text="French", fill="black")
    canvas.itemconfig(word_text, text=current_word["French"], fill="black")
    canvas.itemconfig(bg_image, image=fr_image)
    flip_timer = window.after(3000, func=flip_card)


def flip_card():
    canvas.itemconfig(title_text, text="English", fill="white")
    canvas.itemconfig(word_text, text=current_word["English"], fill="white")
    canvas.itemconfig(bg_image, image=en_image)


def create_to_learn():
    global to_learn
    to_learn.remove(current_word)
    new_word()
    to_learn_list = pandas.DataFrame(to_learn)
    to_learn_list.to_csv("data/to_learn.csv", index=False)


window = Tk()
window.title("Flash card")
window.config(padx=50, pady=50, bg=BACKGROUND_COLOR)
flip_timer = window.after(3000, func=flip_card)

canvas = Canvas(width=800, height=526, bg=BACKGROUND_COLOR, highlightthickness=0)
fr_image = PhotoImage(file="images/card_front.png")
en_image = PhotoImage(file="images/card_back.png")
bg_image = canvas.create_image(400, 263, image=fr_image)
title_text = canvas.create_text(400, 150, text="", font=("Ariel", 40, "italic"))
word_text = canvas.create_text(400, 263, text="", font=("Ariel", 60, "bold"))
canvas.grid(column=0, row=0, columnspan=2)

known_image = PhotoImage(file="images/right.png")
unknown_image = PhotoImage(file="images/wrong.png")
unknown_button = Button(image=unknown_image, highlightthickness=0, command=new_word)
unknown_button.grid(column=0, row=1)
known_button = Button(image=known_image, highlightthickness=0, command=create_to_learn)
known_button.grid(column=1, row=1)
new_word()

window.mainloop()
