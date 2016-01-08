from django.db import models
from django.db.models.signals import pre_delete
from django.dispatch.dispatcher import receiver
import uuid


class Post(models.Model):
    """
    Represents a Post uploaded by a client. Posts consist of the date of creation, latitude / longitude coordinates of
    where the post was created, either a video or image file of the media that was captured, the "lifetime" of the post
    in hours, and a "client secret" that allows the client to make modifications to the post in the future.
    """
    created = models.DateTimeField(auto_now_add=True)
    lat = models.FloatField()
    lng = models.FloatField()
    media_file = models.FileField(upload_to='posts')
    lifetime = models.IntegerField()
    client_secret = models.UUIDField(primary_key=False, default=uuid.uuid4, editable=False)

    class Meta:
        ordering = ('created',)


@receiver(pre_delete, sender=Post)
def post_delete(sender, instance, **kwargs):
    instance.media_file.delete(False)  # delete the models media_file when a Post is deleted


class Comment(models.Model):
    """
    Represents a comment that a client has made on an existing Post. Comments consist of a reference to the Post that
    was commented on, the creation date, and the actual content of the comment.
    """
    post = models.ForeignKey(Post, related_name='comments')
    created = models.DateTimeField(auto_now_add=True)
    content = models.CharField(max_length=200)

    class Meta:
        unique_together = ('post', 'created')
        ordering = ('created',)


class Flag(models.Model):
    """
    Represents a "report flag" on an existing post that requires moderator mediation. Flags consist of a reference to
    the Post that was reported, the creation date, and the reason for reporting.
    """
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
