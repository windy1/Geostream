from django.contrib.auth.models import User
from api.models import Post
from api.serializers import UserSerializer, PostSerializer
from rest_framework.views import APIView
from rest_framework import viewsets
from rest_framework import status
from rest_framework.decorators import api_view, authentication_classes, permission_classes, list_route
from rest_framework.response import Response
from rest_framework.authentication import BasicAuthentication
from rest_framework.permissions import IsAuthenticated


class UserViewSet(viewsets.ModelViewSet):
    queryset = User.objects.all()
    serializer_class = UserSerializer


class PostViewSet(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer
