from tkinter import *
from tkinter import messagebox
from random import randint, choice, shuffle
import pyperclip
import json

def save():
    website = web_entry.get().lower()
    user = user_entry.get()
    psw = psw_entry.get()
    new_data = {
        website: {
            "email": user,
            "password": psw,
        },
    }

    if len(website) == 0 or len(psw) == 0:
        messagebox.showinfo(message="Please don't leave any field empty!")
    else:
        try:
            with open("data.json", "r") as data_file:
                data = json.load(data_file)
        except FileNotFoundError:
            with open("data.json", "w") as data_file:
                json.dump(new_data, data_file, indent=4)
        else:
            data.update(new_data)

            with open("data.json", "w") as data_file:
                json.dump(data, data_file, indent=4)
        finally:
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

def find_website():
    web = web_entry.get().lower()
    try:
        with open("data.json") as data_file:
            data = json.load(data_file)

    except FileNotFoundError:
        messagebox.showinfo(title="Error", message="No data file found")
    else:
        if web in data:
            messagebox.showinfo(title=web,
                                message=f"Email/Username: {data[web]["email"]}\nPassword: {data[web]["password"]}")
        else:
            messagebox.showinfo(message=f"No details for the {web} you entered")



# UI
window = Tk()
window.title("Password Manager")
window.config(padx=50, pady=50)

canvas = Canvas(width=200, height=200)
logo = PhotoImage(file="logo.png")
canvas.create_image(100, 100, image=logo)
canvas.grid(column=1, row=0)

# Entry
web_entry = Entry(width=21)
web_entry.grid(column=1, row=1)
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
search_button = Button(text="Search", width=13, command=find_website)
search_button.grid(column=2, row=1)

# Label
web_label = Label(text="Website:")
web_label.grid(column=0, row=1)
psw_label = Label(text="Password:")
psw_label.grid(column=0, row=3)
user_label = Label(text="Email/Username:")
user_label.grid(column=0, row=2)

window.mainloop()

