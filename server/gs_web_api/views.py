from rest_framework import viewsets
from .models import Post
from .serializers import PostSerializer
from rest_framework import status
from rest_framework.response import Response


class PostViewSet(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer

    def create(self, request):
        print(str(request))
        serializer = PostSerializer(data=request.data)
        if (serializer.is_valid()):
            post = serializer.save()
            data = serializer.data
            data['client_secret'] = post.client_secret
            return Response(data)
        else:
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

    def destroy(self, request, pk=None):
        post = self.get_object()
        if 'HTTP_CLIENTSECRET' in request.META and str(request.META['HTTP_CLIENTSECRET']) == str(post.client_secret):
            return viewsets.ModelViewSet.destroy(self, request, pk)
        else:
            return Response(status=status.HTTP_403_FORBIDDEN)
