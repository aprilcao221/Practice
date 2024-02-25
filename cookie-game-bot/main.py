from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
import time

PLAY_TIME_MIN = 5
chrome_options = webdriver.ChromeOptions()
chrome_options.add_experimental_option(name="detach", value=True)
driver = webdriver.Chrome(options=chrome_options)
driver.get(url="https://orteil.dashnet.org/experiments/cookie/")
cookie = driver.find_element(by=By.ID, value="cookie")

store = driver.find_elements(By.CSS_SELECTOR, value="#store div")
items_cost = []
items_id = []
for item in store:
    item_cost = item.find_element(By.CSS_SELECTOR, value="div b")
    if item_cost.text != '':
        items_id.append(item.get_attribute("id"))
        items_cost.append(int(item_cost.text.split("-")[1].replace(" ", "").replace(",", "")))
print(len(items_cost))
timeout = time.time() + PLAY_TIME_MIN*60
time_to_purchase = time.time() + 10
while True:
    cookie.click()
    if time.time() > time_to_purchase:
        money = int(driver.find_element(By.ID, value="money").text.replace(",", ""))
        for i in range(0, len(items_cost)-1):
            if items_cost[i+1] > money >= items_cost[i]:
                item = driver.find_element(By.ID, value=items_id[i])
                item.click()
        time_to_purchase = time.time() + 10
    if time.time() > timeout:
        break
driver.quit()
