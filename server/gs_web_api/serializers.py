from rest_framework import serializers
from .models import Post, Comment, Flag


class FlagSerializer(serializers.ModelSerializer):
    class Meta:
        model = Flag
        fields = ('post', 'created', 'reason',)


class CommentSerializer(serializers.ModelSerializer):
    class Meta:
        model = Comment
        fields = ('post', 'created', 'content',)


class PostSerializer(serializers.ModelSerializer):
    comments = CommentSerializer(many=True, read_only=True)

    class Meta:
        model = Post
        fields = ('id', 'created', 'lat', 'lng', 'lifetime', 'media_file', 'comments')
