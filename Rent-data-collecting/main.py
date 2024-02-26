from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
import requests
import time

form_url = "https://forms.gle/L3s8F8ZXRTHvn4zn8"
rent_site = "https://appbrewery.github.io/Zillow-Clone/"


response = requests.get(rent_site)
soup = BeautifulSoup(response.text, "html.parser")
links = soup.select(".StyledPropertyCardPhotoBody a")
rent_links = [link.get("href") for link in links]
prices = soup.select(".PropertyCardWrapper span")
rent_prices = [int(price.getText().split("$")[1].split("+")[0].replace(",", "").replace("/mo", "")) for price in prices]
addresses = soup.find_all(name="address")
rent_addresses = [address.getText().replace("|", "").strip() for address in addresses]

chrome_option = webdriver.ChromeOptions()
chrome_option.add_experimental_option("detach", True)
driver = webdriver.Chrome(chrome_option)
driver.get(form_url)
time.sleep(10)
for i in range(len(links)):
    input_box = driver.find_element(By.CSS_SELECTOR, value=".Xb9hP input")
    input_box.click()
    input_box.send_keys(rent_addresses[i], Keys.TAB, rent_prices[i], Keys.TAB, rent_links[i])
    submit = driver.find_element(By.XPATH, value='//*[@id="mG61Hd"]/div[2]/div/div[3]/div[1]/div[1]/div')
    submit.click()
    time.sleep(5)
    submit_another = driver.find_element(By.CSS_SELECTOR, value=".c2gzEf a")
    submit_another.click()
    time.sleep(2)



