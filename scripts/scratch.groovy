import us.wearecurio.model.*
import us.wearecurio.services.*
u = User.get(4)
cs = new CorrelationService()
t1=Tag.get(2)
t2 = Tag.get(135)
s1 = CuriousSeries.create(t1, u.id)
s2 = CuriousSeries.create(t2, u.id)
times = CuriousSeries.mergedTimes(s1, s2)
cs.corDebug(u, t1, t2)

