from selenium import webdriver
from selenium.webdriver.common.by import By
# keep Chrome browser open afer program finishes
chrome_options = webdriver.ChromeOptions()
chrome_options.add_experimental_option("detach", True)
driver = webdriver.Chrome(options=chrome_options)

# driver.get("https://www.amazon.com/dp/B075CYMYK6?ref_=cm_sw_r_cp_ud_ct_FM9M699VKHTT47YD50Q6&th=1")
# price_dollar = driver.find_element(By.CLASS_NAME, value="a-price-whole")
# price_cents = driver.find_element(By.CLASS_NAME, value="a-price-fraction")
# print(f"The price is ${price_dollar.text}.{price_cents.text}")

driver.get("https://python.org")
# search_bar = driver.find_element(By.NAME, value="q")
# button = driver.find_element(By.ID, value="submit")
# doc_link = driver.find_element(By.CSS_SELECTOR, value=".documentation-widget a")
# bug_link = driver.find_element(By.XPATH, value='//*[@id="site-map"]/div[2]/div/ul/li[3]/a')
# print(bug_link.get_property("href"))
# print(button.size)
# print(search_bar.get_attribute("placeholder"))
# print(doc_link.text)
events = driver.find_elements(By.CLASS_NAME, value="event-widget li")
event_dict = {}
for event in events:
    index = events.index(event)
    time = event.find_element(By.TAG_NAME, value="time").get_attribute("datetime").split("T")[0]
    name = event.find_element(By.TAG_NAME, value="a").text
    event_dict[index] = {time: name}
print(event_dict)


driver.quit()