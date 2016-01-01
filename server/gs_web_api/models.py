from django.db import models
import uuid


class Post(models.Model):
    created = models.DateTimeField(auto_now_add=True)
    lat = models.FloatField()
    lng = models.FloatField()
    media_file = models.FileField(upload_to='posts')
    client_secret = models.UUIDField(primary_key=False, default=uuid.uuid4, editable=False)

    class Meta:
        ordering = ('created',)


class Comment(models.Model):
    post = models.ForeignKey(Post, related_name='comments')
    created = models.DateTimeField(auto_now_add=True)
    content = models.CharField(max_length=200)

    class Meta:
        unique_together = ('post', 'created')
        ordering = ('created',)


class Flag(models.Model):
    REASON_INAPPROPRIATE_CONTENT = 'IC'
    REASON_PRIVACY_VIOLATION = 'PV'
    REASON_VIOLENCE_OR_BULLYING = 'VB'
    REASON_SPAM = 'SP'

    REASON_CHOICES = (
        (REASON_INAPPROPRIATE_CONTENT, 'Inappropriate content'),
        (REASON_PRIVACY_VIOLATION, 'Privacy violation'),
        (REASON_VIOLENCE_OR_BULLYING, 'Violence or Bullying'),
        (REASON_SPAM, 'Spam'),
    )

    post = models.ForeignKey(Post, related_name='flags')
    created = models.DateTimeField(auto_now_add=True)
    reason = models.CharField(max_length=2, choices=REASON_CHOICES)
