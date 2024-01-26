MENU = {
    "espresso": {
        "ingredients": {
            "water": 50,
            "coffee": 18,
        },
        "cost": 1.5,
    },
    "latte": {
        "ingredients": {
            "water": 200,
            "milk": 150,
            "coffee": 24,
        },
        "cost": 2.5,
    },
    "cappuccino": {
        "ingredients": {
            "water": 250,
            "milk": 100,
            "coffee": 24,
        },
        "cost": 3.0,
    }
}

resources = {
    "water": 300,
    "milk": 200,
    "coffee": 100,
}


def check_resources(machine, user):
    """input machine resources and user coffee ingredient to get a boolean value"""
    for ingredient in user:
        if machine[ingredient] >= user[ingredient]:
            machine[ingredient] -= user[ingredient]
            return True
        else:
            print(f"Sorry, there is not enough {user[ingredient]}")
            return False


def check_transaction(user_cost):
    """user put the money, compare cost of the coffee user choose to see if they have succeeded the transaction"""
    quarters = int(input("How many quarters($0.25) would you like to put in?: ")) * 0.25
    dimes = int(input("How many dimes($0.1) would you like to put in?: ")) * 0.10
    nickles = int(input("How many nickles($0.05) would you like to put in?: ")) * 0.05
    pennies = int(input("How many pennies($0.01) would you like to put in?: ")) * 0.01
    user_total = quarters + dimes + nickles + pennies
    if user_total >= user_cost:
        change = user_total - user_cost
        print(f"Here's your change: ${change}")
        return True
    else:
        print(f"Sorry it's not enough money, you need ${user_cost}, here's your refund ${user_total}")
        return False


def print_resources():
    print(f"Here's the machine report:\n  "
          f"Water: {resources['water']}ml\n  "
          f"Milk: {resources['milk']}ml\n  "
          f"Coffee: {resources['coffee']}g\n  "
          f"Money:  ${resources['money']}")


def make_coffee():
    user_choice = input("What would you like?: Espresso, Latte or Capuccino\n").lower()
    coffee = MENU[user_choice]
    user_ingredients = coffee['ingredients']
    user_cost = coffee['cost']
    print_resources()
    if check_transaction(user_cost) is True:
        if check_resources(resources, user_ingredients) is True:
            resources['money'] += user_cost
            print_resources()
            print(f"Here's your {user_choice.title()}. Enjoy!")
    else:
        make_coffee()


resources["money"] = 0


machine_off = False


while not machine_off:
    make_coffee()
    if input("Do you want the machine to be turned off?").lower() == "off":
        machine_off = True









