from rest_framework import serializers
from .models import Post
from django.contrib.auth.models import User


class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ('id', 'url', 'username', 'username')
        # makes URLs formatted like '/api/users/<username>/'
        lookup_field = 'username'


class PostSerializer(serializers.ModelSerializer):
    class Meta:
        model = Post
        fields = ('created', 'user', 'lat', 'lng', 'media_file', 'is_video')
