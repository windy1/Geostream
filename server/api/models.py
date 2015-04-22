from django.db import models
from django.contrib.auth.models import User


class Post(models.Model):
    media_file = models.FileField(upload_to='posts')
    is_video = models.BooleanField(default=False)
    user = models.ForeignKey(User)
    lat = models.FloatField()
    lng = models.FloatField()
    post_date = models.DateTimeField(auto_now_add=True)
