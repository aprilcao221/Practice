
import requests

url = "https://api.themoviedb.org/3/movie/73725?language=en-US"

headers = {
    "accept": "application/json",
    "Authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJkOGFkNzEzYzg1OTMyOWZiYmFiNmRlOTRjYzNlYWEwNiIsInN1YiI6IjY1ZTcwZTU2ZWY4YjMyMDE2MmQ3NjM3OSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.X0jXA09mDPLI7NiJq0J2Vwef37oddpTa3QBbtM1E0fo"
}

response = requests.get(url, headers=headers)

print(response.text)