from django.db import models
import uuid


class Post(models.Model):
    created = models.DateTimeField(auto_now_add=True)
    lat = models.FloatField()
    lng = models.FloatField()
    media_file = models.FileField(upload_to='posts')
    is_video = models.BooleanField()
    client_secret = models.UUIDField(primary_key=False, default=uuid.uuid4, editable=False)

    class Meta:
        ordering = ('created',)
