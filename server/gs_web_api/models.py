from django.db import models
from django.contrib.auth.models import User


class Post(models.Model):
    created = models.DateTimeField(auto_now_add=True)
    user = models.ForeignKey(User)
    lat = models.FloatField()
    lng = models.FloatField()
    media_file = models.FileField(upload_to='posts')
    is_video = models.BooleanField()

    class Meta:
        ordering = ('created',)
