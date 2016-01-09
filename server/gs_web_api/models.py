from django.core.validators import MinValueValidator, MaxValueValidator
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
    created = models.DateTimeField(auto_now_add=True)  # date post was created
    lat = models.FloatField()  # latitudinal coordinate
    lng = models.FloatField()  # longitudinal coordinate
    media_file = models.FileField(upload_to='posts')  # file containing content
    lifetime = models.IntegerField(validators=[MinValueValidator(1), MaxValueValidator(24)])  # post lifetime in hours
    # returned on creation for deletion
    client_secret = models.UUIDField(primary_key=False, default=uuid.uuid4, editable=False)

    class Meta:
        ordering = ('created',)


@receiver(pre_delete, sender=Post)
def post_delete(sender, instance, **kwargs):
    """
    Called before a Post is deleted. Cleans up the media_file, deletes associated comments and flags.
    """
    instance.media_file.delete(False)  # delete the models media_file when a Post is deleted

    # delete comments associated with post
    comments = Comment.objects.filter(post=instance.id)
    for comment in comments:
        comment.delete()

    # delete flags associated with post
    flags = Flag.objects.filter(resource_type=Flag.RESOURCE_TYPE_POST).filter(resource_id=instance.id)
    for flag in flags:
        flag.delete()


class Comment(models.Model):
    """
    Represents a comment that a client has made on an existing Post. Comments consist of a reference to the Post that
    was commented on, the creation date, and the actual content of the comment.
    """
    post = models.ForeignKey(Post, related_name='comments')  # the post this comment belongs to
    created = models.DateTimeField(auto_now_add=True)  # date of creation
    content = models.CharField(max_length=200)  # actual content of the comment
    # returned on creation for deletion
    client_secret = models.UUIDField(primary_key=False, default=uuid.uuid4, editable=False)

    class Meta:
        unique_together = ('post', 'created')
        ordering = ('created',)


@receiver(pre_delete, sender=Comment)
def comment_delete(sender, instance, **kwargs):
    """
    Called before a Comment is deleted. Deletes associated flags.
    """
    flags = Flag.objects.filter(resource_type=Flag.RESOURCE_TYPE_COMMENT).filter(resource_id=instance.id)
    for flag in flags:
        flag.delete()


class Flag(models.Model):
    """
    Represents a flag as a result of a resource being reported by a client. A flag consists of the type of resource
    that has been reported, the id of said resource, a creation date, and the reason for the resource being reported.
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

    RESOURCE_TYPE_POST = 'PST'
    RESOURCE_TYPE_COMMENT = 'CMT'

    RESOURCE_TYPE_CHOICES = (
        (RESOURCE_TYPE_POST, 'Post'),
        (RESOURCE_TYPE_COMMENT, 'Comment'),
    )

    resource_type = models.CharField(max_length=3, choices=RESOURCE_TYPE_CHOICES)
    resource_id = models.IntegerField()
    created = models.DateTimeField(auto_now_add=True)  # creation date
    reason = models.CharField(max_length=2, choices=REASON_CHOICES)  # reason for report

    def get_resource(self):
        if self.resource_type == self.RESOURCE_TYPE_POST:
            return Post.get(pk=self.resource_id)
        elif self.resource_type == self.RESOURCE_TYPE_COMMENT:
            return Comment.get(pk=self.resource_id)
