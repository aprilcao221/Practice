import os
from bs4 import BeautifulSoup
import requests
import spotipy
from spotipy.oauth2 import SpotifyOAuth

url = "https://www.billboard.com/charts/hot-100/"
date = input("Which year do you want to travel to? Type the date in this format YYYY-MM-DD\n")
response = requests.get(url=url+date)
web_data = response.text
soup = BeautifulSoup(web_data, "html.parser")
songs = soup.select(selector="li h3")
songs_list = []
for song in songs:
    text = song.getText()
    title = text.replace("\t", "").replace("\n", "")
    songs_list.append(title)



# auth = SpotifyOAuth(client_id="secret", client_secret="secret", redirect_uri="http://example.com")
# response = auth.get_auth_response(open_browser="chrome")
# token = auth.get_access_token(code=response, as_dict=False, check_cache=True)
# print(token)
spotify_endpoint = "https://api.spotify.com/v1/"
token = "BQA6fhibZCDZH-eezIOpyf_7Z6kblgkw4Hv7bv-8yN7R07sHz69Jc46s7JUa-vEv31CNtOFKTfBOkqNX5xXVSOmj9Lyi8CVHTWB8leQPpDuLO36qpz0FuzU-m0f5gw-qBQbCRyLVwRzRTx1Pfiq3EecpVZBGLrM3FpfjYicI7wKLzAxgKJI6OUf9eNpg7H3F7w"
header = {
    'Authorization': f"Bearer {token}"
}
# response = requests.get(url="https://api.spotify.com/v1/me", headers=header)
# user_profile = response.json()
# id = user_profile['id']

id = "secret"
body = {
    "name": "Birthday track",
    "description": "Top 100 songs on the day of your birthday",
    "public": False
}
sp_response = requests.post(url=f"{spotify_endpoint}{id}/playlists", params=body, headers=header)
year = date.split("-")[0]
sp = spotipy.Spotify(auth=token)
song_uris = []
for song in songs_list:
    result = sp.search(q=f"track: {song} year: {year}", type="track")
    try:
        song_id = result["tracks"]["items"][0]["uri"]
        with open(f"{date} uris", "w") as file:
            file.write(f"{song_id}")
    except IndexError:
        print(f"{song} doesn't exit in Spotify, skipped")
    else:
        song_uris.append(song_id)

sp = spotipy.Spotify(
    auth_manager=SpotifyOAuth(
        scope="playlist-modify-private",
        redirect_uri="http://example.com",
        client_id="secret",
        client_secret="secret",
        show_dialog=True,
        cache_path=".cache"
    )
)


playlist = sp.user_playlist_create(user=id, name=f"{date} Billboard 100", public=False)


sp.playlist_add_items(playlist_id=playlist["id"], items=song_uris)




