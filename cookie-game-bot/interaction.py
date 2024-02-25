from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys

driver = webdriver.Chrome()
# driver.get("https://en.wikipedia.org/wiki/Main_Page")
# article_count = driver.find_element(By.CSS_SELECTOR, value="#articlecount a")
# article_count.click()

# all_portals = driver.find_element(By.LINK_TEXT, value="Content portals")
# all_portals.click()

# search = driver.find_element(By.NAME, value="search")
# search.send_keys("Python", Keys.ENTER)

driver.get("http://secure-retreat-92358.herokuapp.com/")
name = driver.find_element(By.NAME, value="fName")
name.send_keys("April", Keys.TAB, "C", Keys.TAB, "aprilc@gmail.com")
button = driver.find_element(By.TAG_NAME, "button")
button.click()