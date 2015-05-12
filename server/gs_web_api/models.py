from django.db import models


class Post(models.Model):
    created = models.DateTimeField(auto_now_add=True)
    lat = models.FloatField()
    lng = models.FloatField()
    media_file = models.FileField(upload_to='posts')
    is_video = models.BooleanField()

    class Meta:
        ordering = ('created',)
