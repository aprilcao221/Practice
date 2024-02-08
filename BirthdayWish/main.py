##################### Extra Hard Starting Project ######################

# 1. Update the birthdays.csv

# 2. Check if today matches a birthday in the birthdays.csv

# 3. If step 2 is true, pick a random letter from letter templates and replace the [NAME] with the person's actual name from birthdays.csv

# 4. Send the letter generated in step 3 to that person's email address.

import pandas
from datetime import datetime
from random import randint
import smtplib

now = datetime.now()
month = now.month
day = now.day

data = pandas.read_csv("birthdays.csv")
birthdays = data.to_dict(orient="records")
birthday_today = [person for person in birthdays if person["month"] == month and person["day"] == day]

for person in birthday_today:
    with open(f"letter_templates/letter_{randint(1,3)}.txt") as letter:
        content = letter.read()
        new_content = content.replace("[NAME]", f"{person["name"]}")
    with smtplib.SMTP("smtp.163.com", 25) as connection:
        connection.starttls()
        connection.login(user="caomancomeon@163.com", password="TJWJFXASKQGBHELS")
        connection.sendmail(from_addr="caomancomeon@163.com",
                            to_addrs=person["email"],
                            msg=f"Subject:Happy Birthday!\n\n{new_content}")





