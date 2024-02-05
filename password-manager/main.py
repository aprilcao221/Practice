from tkinter import *
from tkinter import messagebox
from random import randint, choice, shuffle
import pyperclip

def save():
    website = web_entry.get()
    user = user_entry.get()
    psw = psw_entry.get()

    if len(website) == 0 or len(psw) == 0:
        messagebox.showinfo(message="Please don't leave any field empty!")
    else:
        is_ok = messagebox.askokcancel(title=website, message=f"These are the details you entered: \nEmail/Username: {user}\n"
                                                  f"Password: {psw}\n Is it ok to save?")
        if is_ok:
            with open("data.txt", "w") as file:
                file.write(f"{website} | {user} | {psw}\n")
            web_entry.delete(0, END)
            user_entry.delete(0, END)
            user_entry.insert(0, "caomancomeon@gmail.com")
            psw_entry.delete(0, END)

def generate_psw():
    letters = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
               'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
               'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z']
    numbers = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9']
    symbols = ['!', '#', '$', '%', '&', '(', ')', '*', '+']

    char_list = [choice(letters) for n in range(randint(8, 10))]
    sym_list = [choice(symbols) for n in range(randint(2, 4))]
    num_list = [choice(numbers) for n in range(randint(2, 4))]
    psw_list = char_list + sym_list + num_list
    shuffle(psw_list)
    password = "".join(psw_list)
    psw_entry.insert(0, password)
    pyperclip.copy(password)

# UI
window = Tk()
window.title("Password Manager")
window.config(padx=50, pady=50)

canvas = Canvas(width=200, height=200)
logo = PhotoImage(file="logo.png")
canvas.create_image(100, 100, image=logo)
canvas.grid(column=1, row=0)

# Entry
web_entry = Entry(width=38)
web_entry.grid(column=1, row=1, columnspan=2)
web_entry.focus()
psw_entry = Entry(width=21)
psw_entry.grid(column=1, row=3)
user_entry = Entry(width=38)
user_entry.grid(column=1, row=2, columnspan=2)
user_entry.insert(0, "caomancomeon@gmail.com")

# Button
psw_button = Button(text="Generate Password", command=generate_psw)
psw_button.grid(column=2, row=3)
add_button = Button(text="Add", width=36, command=save)
add_button.grid(column=1, row=4, columnspan=2)

# Label
web_label = Label(text="Website:")
web_label.grid(column=0, row=1)
psw_label = Label(text="Password:")
psw_label.grid(column=0, row=3)
user_label = Label(text="Email/Username:")
user_label.grid(column=0, row=2)

window.mainloop()

