from rest_framework import viewsets
from .models import Post, Comment
from .serializers import PostSerializer, CommentSerializer
from rest_framework import status
from rest_framework.response import Response


class PostViewSet(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer

    def create(self, request):
        # hijack create method to include the client secret on initial creation
        serializer = PostSerializer(data=request.data)
        if (serializer.is_valid()):
            post = serializer.save()
            data = serializer.data
            data['client_secret'] = post.client_secret
            return Response(data)
        else:
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

    def destroy(self, request, pk=None):
        # hijack destroy method to make sure that the request include the client secret
        post = self.get_object()
        if 'HTTP_CLIENTSECRET' in request.META and str(request.META['HTTP_CLIENTSECRET']) == str(post.client_secret):
            return viewsets.ModelViewSet.destroy(self, request, pk)
        else:
            return Response(status=status.HTTP_403_FORBIDDEN)


class CommentViewSet(viewsets.ModelViewSet):
    queryset = Comment.objects.all()
    serializer_class = CommentSerializer
